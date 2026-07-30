package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.dto.ClmSignRequestDto;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.global.error.ApiException;
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

    public ClmDocumentService(ClmDocumentRepository clmDocumentRepository,
                              VolunteerRepository volunteerRepository,
                              ModusignApiClient modusignApiClient) {
        this.clmDocumentRepository = clmDocumentRepository;
        this.volunteerRepository = volunteerRepository;
        this.modusignApiClient = modusignApiClient;
    }

    @Transactional
    public ClmDocumentResponseDto requestSign(ClmSignRequestDto request, Long currentUserId) {
        Volunteer volunteer = volunteerRepository.findById(request.getVolunteerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VOLUNTEER_NOT_FOUND", "해당 봉사/기부 공고를 찾을 수 없습니다."));

        String docTitle = "[" + volunteer.getCategory() + "] " + volunteer.getTitle() + " 참여/후원 신청 동의서";

        ModusignApiClient.ModusignRequestResult signResult = modusignApiClient.requestSigning(
                docTitle,
                request.getApplicantName(),
                request.getApplicantEmail()
        );

        ClmDocument doc = new ClmDocument(
                volunteer.getId(),
                volunteer.getTitle(),
                currentUserId,
                request.getApplicantName(),
                request.getApplicantEmail(),
                request.getApplicantPhone(),
                signResult.documentId(),
                signResult.signingUrl()
        );

        ClmDocument saved = clmDocumentRepository.save(doc);
        return ClmDocumentResponseDto.fromEntity(saved);
    }

    @Transactional
    public ClmDocumentResponseDto completeSign(Long documentId) {
        ClmDocument doc = clmDocumentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLM_DOCUMENT_NOT_FOUND", "전자서명 서류를 찾을 수 없습니다."));

        doc.updateStatusToSigned();
        return ClmDocumentResponseDto.fromEntity(doc);
    }

    public ClmDocumentResponseDto getDocumentDetail(Long documentId) {
        ClmDocument doc = clmDocumentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLM_DOCUMENT_NOT_FOUND", "전자서명 서류를 찾을 수 없습니다."));

        return ClmDocumentResponseDto.fromEntity(doc);
    }

    public List<ClmDocumentResponseDto> getMyDocuments(String email) {
        return clmDocumentRepository.findByApplicantEmailAndIsDeletedFalseOrderByIdDesc(email)
                .stream()
                .map(ClmDocumentResponseDto::fromEntity)
                .toList();
    }
}
