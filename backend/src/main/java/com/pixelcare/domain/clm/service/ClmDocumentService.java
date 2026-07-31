package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.dto.ClmSignRequestDto;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.volunteer.Volunteer;
import com.pixelcare.volunteer.VolunteerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClmDocumentService {

    private final ClmDocumentRepository clmDocumentRepository;
    private final VolunteerRepository volunteerRepository;
    private final ModusignApiClient modusignApiClient;
    private final ClmDocumentArchiveService archiveService;

    public ClmDocumentService(ClmDocumentRepository clmDocumentRepository,
                              VolunteerRepository volunteerRepository,
                              ModusignApiClient modusignApiClient,
                              ClmDocumentArchiveService archiveService) {
        this.clmDocumentRepository = clmDocumentRepository;
        this.volunteerRepository = volunteerRepository;
        this.modusignApiClient = modusignApiClient;
        this.archiveService = archiveService;
    }

    @Transactional
    public ClmDocumentResponseDto requestSign(ClmSignRequestDto request, CurrentUser currentUser) {
        Volunteer volunteer = volunteerRepository.findById(request.getVolunteerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VOLUNTEER_NOT_FOUND", "해당 봉사/기부 공고를 찾을 수 없습니다."));

        String docTitle = "[" + volunteer.getCategory() + "] " + volunteer.getTitle() + " 참여/후원 신청 동의서";

        ModusignApiClient.ModusignRequestResult signResult = modusignApiClient.requestSigning(
                docTitle,
                request.getApplicantName(),
                currentUser.email()
        );

        ClmDocument doc = new ClmDocument(
                volunteer.getId(),
                volunteer.getTitle(),
                currentUser.id(),
                request.getApplicantName(),
                currentUser.email(),
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
    public void applyWebhookEvent(String modusignDocumentId, String eventType) {
        clmDocumentRepository.findByModusignDocumentIdAndIsDeletedFalse(modusignDocumentId)
                .ifPresent(document -> {
                    document.applyModusignEvent(eventType);
                    if ("document_all_signed".equals(eventType)) {
                        archiveService.archiveCompletedFiles(document);
                    }
                });
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
            document.applyModusignEvent("document_all_signed");
        } catch (ApiException e) {
            if ("MODUSIGN_DOCUMENT_NOT_COMPLETED".equals(e.getCode())) {
                return;
            }
            throw e;
        }
    }

    private ClmDocument findOwnedDocument(Long documentId, CurrentUser currentUser) {
        ClmDocument doc = clmDocumentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLM_DOCUMENT_NOT_FOUND", "전자서명 서류를 찾을 수 없습니다."));
        boolean operator = currentUser.hasRole("OPERATOR") || currentUser.hasRole("ROLE_OPERATOR");
        if (!operator && !currentUser.id().equals(doc.getApplicantUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CLM_DOCUMENT_FORBIDDEN", "해당 전자서명 서류를 볼 권한이 없습니다.");
        }
        return doc;
    }
}
