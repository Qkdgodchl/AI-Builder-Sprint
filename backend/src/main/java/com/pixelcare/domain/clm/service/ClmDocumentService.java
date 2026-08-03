package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.ai.dto.PledgeIntent;
import com.pixelcare.domain.ai.repository.AiConsultationRepository;
import com.pixelcare.domain.ai.service.AiConsultationService;
import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.dto.ClmSignRequestDto;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.domain.clm.repository.ClmCommitmentRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentAccessRepository;
import com.pixelcare.domain.clm.repository.WebhookEventRepository;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClmDocumentService {

    private final ClmDocumentRepository clmDocumentRepository;
    private final ClmCommitmentRepository commitmentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ModusignApiClient modusignApiClient;
    private final ClmDocumentArchiveService archiveService;
    private final ClmDocumentAccessService accessService;
    private final ClmDocumentAccessRepository accessRepository;
    private final AiConsultationRepository consultationRepository;
    private final AiConsultationService consultationService;
    private final PledgeContractPdfGenerator pdfGenerator;
    private final ClmCompletionMessageService completionMessageService;
    private final com.pixelcare.domain.user.service.WarmthService warmthService;

    public ClmDocumentService(ClmDocumentRepository clmDocumentRepository,
                              ClmCommitmentRepository commitmentRepository,
                              WebhookEventRepository webhookEventRepository,
                              ModusignApiClient modusignApiClient,
                              ClmDocumentArchiveService archiveService,
                              ClmDocumentAccessService accessService,
                              ClmDocumentAccessRepository accessRepository,
                              AiConsultationRepository consultationRepository,
                              AiConsultationService consultationService,
                              PledgeContractPdfGenerator pdfGenerator,
                              ClmCompletionMessageService completionMessageService,
                              com.pixelcare.domain.user.service.WarmthService warmthService) {
        this.clmDocumentRepository = clmDocumentRepository;
        this.commitmentRepository = commitmentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.modusignApiClient = modusignApiClient;
        this.archiveService = archiveService;
        this.accessService = accessService;
        this.accessRepository = accessRepository;
        this.consultationRepository = consultationRepository;
        this.consultationService = consultationService;
        this.pdfGenerator = pdfGenerator;
        this.completionMessageService = completionMessageService;
        this.warmthService = warmthService;
    }

    @Transactional
    public ClmDocumentResponseDto requestSign(ClmSignRequestDto request, CurrentUser currentUser) {
        ClmCommitmentRepository.CommitmentSigningContext commitment =
                commitmentRepository.requireOwnedSigningContext(request.getCommitmentPublicId(), currentUser.id());
        var existingDoc = clmDocumentRepository.findByCommitmentIdAndIsDeletedFalse(commitment.id());
        if (existingDoc.isPresent()) {
            /*
             * 서명이 끝난 문서인데 약정이 다시 서명 대기로 돌아왔다면 조건이 바뀐 것이다.
             * 옛 내용 그대로인 약정서를 다시 열어 줄 수는 없다. 게다가 이미 서명된
             * 문서라 모두싸인에서도 "문서에 접근할 수 없습니다"로 막힌다.
             * 지난 문서는 보관만 하고 새 약정서를 만든다.
             */
            ClmDocument previous = existingDoc.get();
            boolean supersededByTermChange = "SIGNED".equals(previous.getStatus())
                    && "SIGNING".equals(commitment.status());
            if (!supersededByTermChange) {
                // 서명 링크는 10분이면 만료된다. 예전 주소를 그대로 돌려주면
                // 서명창을 열자마자 "문서에 접근할 수 없습니다"로 끝난다.
                return ClmDocumentResponseDto.fromEntity(renewSigningLinkIfExpired(previous));
            }
            previous.markDeleted("RESIGN_AFTER_TERM_CHANGE");
            clmDocumentRepository.save(previous);
        }

        Long consultationId = commitmentRepository.findConsultationIdByCommitmentId(commitment.id());
        if (consultationId != null) {
            return requestSignFromConversation(consultationId, request.getCommitmentPublicId(), request.getApplicantPhone(), currentUser);
        }

        String applicantName = commitment.applicantName() == null || commitment.applicantName().isBlank()
                ? currentUser.nickname() : commitment.applicantName();

        // LLM 상담 ID가 없는 일반 신청도 iText 8로 커스텀 약정서 PDF를 생성하여 모두싸인에 업로드
        String pledgeType = resolvePledgeType(commitment.opportunityType(), "VOLUNTEER");
        boolean heritage = isHeritagePledge(pledgeType);

        // 약정서에 찍히는 금액은 사용자가 신청할 때 정한 값이어야 한다.
        // 여기서 임의의 값을 넣으면 실제 약정과 다른 금액으로 서명을 받게 된다.
        java.math.BigDecimal pledgeAmount = commitment.pledgeAmount();
        PledgeIntent intent = new PledgeIntent(
                pledgeType,
                commitment.title(),
                pledgeAmount,
                commitment.pledgeFrequency() != null ? commitment.pledgeFrequency() : "MONTHLY",
                commitment.effectiveFrom() != null ? commitment.effectiveFrom() : java.time.LocalDate.now(),
                "부산광역시",
                "NONE",
                true,
                true,
                "정식 약정서 체결",
                "HOMETOWN_DONATION".equals(pledgeType) ? "부산 동백전 지역화폐" : null,
                "HOMETOWN_DONATION".equals(pledgeType) ? "26000" : null,
                hometownTaxCredit(pledgeType, pledgeAmount),
                heritage ? commitment.title() : null,
                heritage ? "사후 유산 유증 기부 약정 (유언 공증 체결)" : null,
                java.util.List.of()
        );

        byte[] pdfBytes = pdfGenerator.generate(
                intent,
                java.util.List.<String[]>of(new String[]{"USER", commitment.title() + " 신청 약정서"}),
                applicantName,
                commitment.applicantEmail(),
                commitment.title(),
                commitment.organizer() != null ? commitment.organizer() : "잇다 지정 기관"
        );

        ModusignApiClient.ModusignRequestResult signResult = modusignApiClient.uploadAndRequestSigning(
                pdfBytes,
                commitment.title(),
                applicantName,
                commitment.applicantEmail()
        );
        Long signatureRequestId = commitmentRepository.createSignatureRequest(
                commitment, currentUser.id(), commitment.applicantEmail(), signResult.documentId());

        ClmDocument doc = new ClmDocument(
                commitment.id(),
                signatureRequestId,
                commitment.opportunityId(),
                commitment.title(),
                currentUser.id(),
                applicantName,
                commitment.applicantEmail(),
                request.getApplicantPhone(),
                signResult.documentId(),
                signResult.participantId(),
                signResult.templateId() != null ? signResult.templateId() : "PDF_UPLOAD",
                signResult.signingUrl(),
                signResult.signingUrlExpiresAt()
        );

        ClmDocument saved = clmDocumentRepository.save(doc);
        archiveService.archiveDraftPdf(saved, pdfBytes);
        return ClmDocumentResponseDto.fromEntity(saved);
    }

    @Transactional
    /** 만료된 서명 링크를 새로 받아 둔다. 아직 살아 있으면 그대로 쓴다. */
    private ClmDocument renewSigningLinkIfExpired(ClmDocument document) {
        java.time.LocalDateTime expiresAt = document.getSigningUrlExpiresAt();
        if (expiresAt != null && expiresAt.isAfter(java.time.LocalDateTime.now())) {
            return document;
        }
        try {
            ModusignApiClient.SecureLinkResult link = modusignApiClient.createSecureLink(
                    document.getModusignDocumentId(), document.getModusignParticipantId()
            );
            document.updateSecureLink(link.signingUrl(), link.expiresAt());
        } catch (RuntimeException error) {
            // 재발급이 막혀도 기존 주소로라도 열어 볼 수 있게 그대로 둔다.
            System.err.println("서명 링크 재발급 실패, 기존 주소를 유지합니다: " + error.getMessage());
        }
        return document;
    }

    public ClmDocumentResponseDto refreshSecureLink(Long documentId, CurrentUser currentUser) {
        ClmDocument doc = findOwnedDocument(documentId, currentUser);
        ModusignApiClient.SecureLinkResult link = modusignApiClient.createSecureLink(
                doc.getModusignDocumentId(), doc.getModusignParticipantId()
        );
        doc.updateSecureLink(link.signingUrl(), link.expiresAt());
        return ClmDocumentResponseDto.fromEntity(doc);
    }

    @Transactional
    public ClmDocumentResponseDto getDocumentDetail(Long documentId, CurrentUser currentUser) {
        ClmDocument document = findOwnedDocument(documentId, currentUser);
        synchronizeModusignStatus(document);
        return ClmDocumentResponseDto.fromEntity(document);
    }

    public List<ClmDocumentResponseDto> getMyDocuments(CurrentUser currentUser) {
        return clmDocumentRepository.findByApplicantUserIdAndIsDeletedFalseOrderByIdDesc(currentUser.id())
                .stream()
                .map(ClmDocumentResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public List<ClmDocumentResponseDto> getManagerApplicationDocuments(
            String applicationPublicId,
            CurrentUser manager
    ) {
        List<Long> documentIds = accessRepository.findAccessibleDocumentIds(
                manager.id(), applicationPublicId
        );
        return documentIds.stream()
                .map(id -> clmDocumentRepository.findByIdAndIsDeletedFalse(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(document -> {
                    synchronizeModusignStatus(document);
                    return ClmDocumentResponseDto.fromEntity(document);
                })
                .toList();
    }

    /**
     * 조직 전체 문서 목록. 대시보드 카드 클릭용이라 문서 수가 많을 수 있어
     * 외부 API를 타는 상태 동기화 없이 저장된 상태만 빠르게 돌려준다.
     */
    @Transactional(readOnly = true)
    public List<ClmDocumentResponseDto> getOrganizationDocuments(Long organizationId, CurrentUser manager) {
        return accessRepository.findAccessibleDocumentIdsByOrganization(manager.id(), organizationId)
                .stream()
                .map(id -> clmDocumentRepository.findByIdAndIsDeletedFalse(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(ClmDocumentResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public void applyWebhookEvent(String eventId, String modusignDocumentId, String eventType, String payload) {
        if (!webhookEventRepository.start("MODUSIGN", eventId, eventType, payload)) return;
        try {
            clmDocumentRepository.findByModusignDocumentIdAndIsDeletedFalse(modusignDocumentId)
                    .ifPresent(document -> {
                        boolean applied = document.applyModusignEvent(eventType);
                        if (!applied) return;
                        commitmentRepository.applySignatureState(
                                document.getCommitmentId(), document.getSignatureRequestId(), signatureState(eventType));
                        if ("document_all_signed".equals(eventType)) {
                            archiveService.archiveCompletedFiles(document);
                            completionMessageService.attachTo(document);
                        }
                    });
            webhookEventRepository.complete("MODUSIGN", eventId);
        } catch (RuntimeException error) {
            webhookEventRepository.fail("MODUSIGN", eventId, error.getMessage());
            throw error;
        }
    }

    /** 서명이 확정되면 약정 상태를 넘기고, 체결한 회원의 온기를 올린다. */
    private void markSigned(ClmDocument document) {
        commitmentRepository.applySignatureState(
                document.getCommitmentId(), document.getSignatureRequestId(), "SIGNED");
        warmthService.awardQuietly(document.getApplicantUserId(),
                com.pixelcare.domain.user.service.WarmthService.Reason.COMMITMENT_SIGNED);
    }

    private void synchronizeModusignStatus(ClmDocument document) {
        boolean settled = "SIGNED".equals(document.getStatus())
                || "REJECTED".equals(document.getStatus())
                || "CANCELED".equals(document.getStatus());

        if (!settled) {
            try {
                archiveService.archiveCompletedFiles(document);
                if (document.applyModusignEvent("document_all_signed")) {
                    markSigned(document);
                }
            } catch (Exception e) {
                // 모두싸인 이용한도가 막혀 시뮬레이션으로 발급한 데모 문서만 완료로 넘긴다.
                if (document.getModusignDocumentId() != null && document.getModusignDocumentId().startsWith("MODU_SIGNED_")) {
                    if (document.applyModusignEvent("document_all_signed")) {
                        markSigned(document);
                    }
                    return;
                }
                /*
                 * 조회에 실패했다고 서명이 끝났다고 볼 수는 없다.
                 * 호출 한도(429)나 일시적인 네트워크 오류에도 완료로 넘겨 버리면
                 * 아무도 서명하지 않은 약정이 체결로 남고 온기까지 오른다.
                 * 상태를 그대로 두면 다음 조회나 웹훅에서 실제 상태로 맞춰진다.
                 */
                System.err.println("모두싸인 상태 동기화 실패, 상태를 유지한다: " + e.getMessage());
            }
        }
        // 웹훅으로 먼저 완료 처리된 약정도 감사 메시지를 받을 수 있도록
        // 상태와 무관하게 시도한다. 서명 전이거나 이미 남긴 경우는 내부에서 걸러진다.
        completionMessageService.attachTo(document);
    }

    private ClmDocument findOwnedDocument(Long documentId, CurrentUser currentUser) {
        return accessService.requireAccess(documentId, currentUser);
    }

    /**
     * 공고 종류를 약정 유형으로 옮긴다.
     * 유형 이름은 AI 구조화(UpstageApiClient)와 약정서 서식이 함께 쓰는 값이라
     * 한 곳에서만 정해 두고 양쪽 서명 경로가 같은 표를 보게 한다.
     */
    private String resolvePledgeType(String opportunityType, String fallback) {
        if (opportunityType == null) return fallback;
        return switch (opportunityType.toUpperCase()) {
            case "HOMETOWN" -> "HOMETOWN_DONATION";
            case "LEGACY" -> "LEGACY_DONATION";
            case "HERITAGE", "UNESCO" -> "CULTURAL_HERITAGE_DONATION";
            case "VOLUNTEER" -> "VOLUNTEER";
            case "DONATION" -> "DONATION";
            default -> fallback;
        };
    }

    private boolean isHeritagePledge(String pledgeType) {
        return "LEGACY_DONATION".equals(pledgeType) || "CULTURAL_HERITAGE_DONATION".equals(pledgeType);
    }

    /**
     * 고향사랑기부는 10만원까지 전액 세액공제된다.
     * 약정 금액을 모르면 공제액도 적을 수 없으므로 비워 둔다.
     */
    private java.math.BigDecimal hometownTaxCredit(String pledgeType, java.math.BigDecimal amount) {
        if (!"HOMETOWN_DONATION".equals(pledgeType) || amount == null) return null;
        return amount.min(new java.math.BigDecimal("100000"));
    }

    private String signatureState(String eventType) {
        return switch (eventType) {
            case "document_started" -> "SIGNING";
            case "document_signed" -> "PARTIALLY_SIGNED";
            case "document_all_signed" -> "SIGNED";
            case "document_rejected" -> "REJECTED";
            case "document_request_canceled", "document_signing_canceled" -> "CANCELED";
            default -> "REQUESTED";
        };
    }

    /**
     * LLM 대화 기반 약정서 전자서명 요청:
     * 1. AI 상담 세션에서 추출된 PledgeIntent + 대화 이력 조회
     * 2. iText로 약정서 PDF 생성
     * 3. 모두싸인에 PDF 직접 업로드 → 서명 요청 생성
     * 4. CLM 문서 레코드 저장
     */
    @Transactional
    public ClmDocumentResponseDto requestSignFromConversation(
            Long consultationId,
            String commitmentPublicId,
            String applicantPhone,
            CurrentUser currentUser
    ) {
        // 1. 상담 세션에서 PledgeIntent + 대화 이력 조회
        var consultationResponse = consultationService.get(consultationId, currentUser.id());
        PledgeIntent intent = consultationResponse.intent();
        if (intent == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLEDGE_INTENT_MISSING",
                    "AI가 정리한 약정 의사가 없습니다. 먼저 약정 의사를 정리해주세요.");
        }
        List<String[]> messages = consultationRepository.findMessages(consultationId);

        ClmCommitmentRepository.CommitmentSigningContext commitment =
                commitmentRepository.requireOwnedSigningContext(commitmentPublicId, currentUser.id());

        // 프로그램 카테고리에 맞춰 pledgeType 자동 보완
        String pledgeType = resolvePledgeType(commitment.opportunityType(), intent.pledgeType());

        String organizerName = commitment.organizer() != null && !commitment.organizer().isBlank()
                ? commitment.organizer()
                : (intent.beneficiary() != null ? intent.beneficiary() : "잇다 지정 기관");

        intent = new PledgeIntent(
                pledgeType,
                organizerName,
                intent.amount(),
                intent.frequency(),
                intent.startDate(),
                intent.region(),
                intent.rewardPreference(),
                intent.taxDeductionConsent(),
                intent.privacyConsent(),
                intent.specialConditions(),
                intent.giftItem(),
                intent.localGovCode(),
                intent.taxCreditAmount(),
                intent.heritageTarget(),
                intent.bequestType(),
                intent.missingFields()
        );

        String applicantName = commitment.applicantName() == null || commitment.applicantName().isBlank()
                ? currentUser.nickname() : commitment.applicantName();
        String applicantEmail = commitment.applicantEmail();

        // 3. iText 약정서 PDF 생성
        byte[] pdfBytes = pdfGenerator.generate(
                intent,
                messages,
                applicantName,
                applicantEmail,
                commitment.title(),
                organizerName
        );

        // 4. 모두싸인에 PDF 업로드 → 서명 요청
        ModusignApiClient.ModusignRequestResult signResult = modusignApiClient.uploadAndRequestSigning(
                pdfBytes,
                commitment.title(),
                applicantName,
                applicantEmail
        );

        // 5. 서명 요청 레코드 업데이트 또는 신규 저장
        Long signatureRequestId = commitmentRepository.createSignatureRequest(
                commitment, currentUser.id(), applicantEmail, signResult.documentId());

        var existingDocOpt = clmDocumentRepository.findByCommitmentIdAndIsDeletedFalse(commitment.id());
        ClmDocument doc;
        if (existingDocOpt.isPresent()) {
            doc = existingDocOpt.get();
            doc.updateSigningSession(
                    signatureRequestId,
                    signResult.documentId(),
                    signResult.participantId(),
                    signResult.signingUrl(),
                    signResult.signingUrlExpiresAt()
            );
        } else {
            doc = new ClmDocument(
                    commitment.id(),
                    signatureRequestId,
                    commitment.opportunityId(),
                    commitment.title(),
                    currentUser.id(),
                    applicantName,
                    applicantEmail,
                    applicantPhone,
                    signResult.documentId(),
                    signResult.participantId(),
                    signResult.templateId() != null ? signResult.templateId() : "PDF_UPLOAD",
                    signResult.signingUrl(),
                    signResult.signingUrlExpiresAt()
            );
        }

        ClmDocument saved = clmDocumentRepository.save(doc);
        archiveService.archiveDraftPdf(saved, pdfBytes);
        return ClmDocumentResponseDto.fromEntity(saved);
    }
}
