package com.pixelcare.domain.clm.service;

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

    public ClmDocumentService(ClmDocumentRepository clmDocumentRepository,
                              ClmCommitmentRepository commitmentRepository,
                              WebhookEventRepository webhookEventRepository,
                              ModusignApiClient modusignApiClient,
                              ClmDocumentArchiveService archiveService,
                              ClmDocumentAccessService accessService,
                              ClmDocumentAccessRepository accessRepository) {
        this.clmDocumentRepository = clmDocumentRepository;
        this.commitmentRepository = commitmentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.modusignApiClient = modusignApiClient;
        this.archiveService = archiveService;
        this.accessService = accessService;
        this.accessRepository = accessRepository;
    }

    @Transactional
    public ClmDocumentResponseDto requestSign(ClmSignRequestDto request, CurrentUser currentUser) {
        ClmCommitmentRepository.CommitmentSigningContext commitment =
                commitmentRepository.requireOwnedSigningContext(request.getCommitmentPublicId(), currentUser.id());
        clmDocumentRepository.findByCommitmentIdAndIsDeletedFalse(commitment.id()).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "SIGNATURE_ALREADY_REQUESTED", "이미 전자서명을 요청한 약정입니다.");
        });
        String applicantName = commitment.applicantName() == null || commitment.applicantName().isBlank()
                ? currentUser.nickname() : commitment.applicantName();

        ModusignApiClient.ModusignRequestResult signResult = modusignApiClient.requestSigning(
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
                signResult.templateId(),
                signResult.signingUrl(),
                signResult.signingUrlExpiresAt()
        );

        ClmDocument saved = clmDocumentRepository.save(doc);
        return ClmDocumentResponseDto.fromEntity(saved);
    }

    @Transactional
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
                        }
                    });
            webhookEventRepository.complete("MODUSIGN", eventId);
        } catch (RuntimeException error) {
            webhookEventRepository.fail("MODUSIGN", eventId, error.getMessage());
            throw error;
        }
    }

    private void synchronizeModusignStatus(ClmDocument document) {
        if ("SIGNED".equals(document.getStatus())
                || "REJECTED".equals(document.getStatus())
                || "CANCELED".equals(document.getStatus())) {
            return;
        }

        try {
            // archiveCompletedFiles 내부의 단 한 번의 문서 상세 조회로
            // 완료 상태 확인과 PDF/감사추적인증서 다운로드를 함께 처리한다.
            archiveService.archiveCompletedFiles(document);
            if (document.applyModusignEvent("document_all_signed")) {
                commitmentRepository.applySignatureState(
                        document.getCommitmentId(), document.getSignatureRequestId(), "SIGNED");
            }
        } catch (ApiException e) {
            if ("MODUSIGN_DOCUMENT_NOT_COMPLETED".equals(e.getCode())) {
                return;
            }
            throw e;
        }
    }

    private ClmDocument findOwnedDocument(Long documentId, CurrentUser currentUser) {
        return accessService.requireAccess(documentId, currentUser);
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
}
