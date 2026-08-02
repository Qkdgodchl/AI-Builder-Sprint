package com.pixelcare.domain.clm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.service.UpstageDocumentClient;
import com.pixelcare.domain.clm.dto.ClmVerificationResponse;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.entity.ClmDocumentVerification;
import com.pixelcare.domain.clm.repository.ClmCommitmentRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentVerificationRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 보관된 약정서 파일을 되읽어, 우리가 들고 있는 약정 값과 같은지 맞춰 본다.
 *
 * 서명이 끝났다는 사실만으로는 "무엇에 서명했는지"를 보이지 못한다.
 * 체결본에서 직접 읽어낸 항목을 나란히 놓아야 다툼이 생겼을 때 근거가 된다.
 */
@Service
public class ClmDocumentVerificationService {

    private static final String MATCHED = "MATCHED";
    private static final String MISMATCHED = "MISMATCHED";
    private static final String UNREADABLE = "UNREADABLE";

    private final ClmDocumentAccessService accessService;
    private final ClmDocumentArchiveService archiveService;
    private final ClmCommitmentRepository commitmentRepository;
    private final ClmDocumentVerificationRepository verificationRepository;
    private final UpstageDocumentClient documentClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClmDocumentVerificationService(ClmDocumentAccessService accessService,
                                          ClmDocumentArchiveService archiveService,
                                          ClmCommitmentRepository commitmentRepository,
                                          ClmDocumentVerificationRepository verificationRepository,
                                          UpstageDocumentClient documentClient) {
        this.accessService = accessService;
        this.archiveService = archiveService;
        this.commitmentRepository = commitmentRepository;
        this.verificationRepository = verificationRepository;
        this.documentClient = documentClient;
    }

    @Transactional(readOnly = true)
    public Optional<ClmVerificationResponse> findLatest(Long documentId, CurrentUser user) {
        accessService.requireAccess(documentId, user);
        return verificationRepository
                .findFirstByClmDocumentIdAndIsDeletedFalseOrderByIdDesc(documentId)
                .map(this::toResponse);
    }

    @Transactional
    public ClmVerificationResponse verify(Long documentId, CurrentUser user) {
        ClmDocument document = accessService.requireAccess(documentId, user);

        if (!documentClient.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "UPSTAGE_NOT_CONFIGURED",
                    "문서 검증에 필요한 Upstage 설정(UPSTAGE_API_KEY)이 없습니다.");
        }

        ClmDocumentArchiveService.ArchivedPdf archived = archiveService.readForVerification(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLM_FILE_NOT_FOUND",
                        "검증할 약정서 파일이 아직 보관되지 않았습니다."));

        ClmCommitmentRepository.GratitudeContext source =
                commitmentRepository.findGratitudeContext(document.getCommitmentId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMITMENT_NOT_FOUND",
                                "대조할 약정 정보를 찾을 수 없습니다."));

        String excerpt = documentClient.parseText(archived.bytes(), archived.filename())
                .map(text -> text.length() > 1500 ? text.substring(0, 1500) : text)
                .orElse(null);

        Optional<UpstageDocumentClient.ExtractedPledge> extracted =
                documentClient.extractPledge(archived.bytes());

        if (extracted.isEmpty()) {
            ClmDocumentVerification saved = verificationRepository.save(new ClmDocumentVerification(
                    documentId, UNREADABLE, 0, 0, "[]", excerpt, archived.fileType(), "UPSTAGE_DOCUMENT_AI"));
            return toResponse(saved);
        }

        List<FieldCheck> checks = compare(source, document.getApplicantName(), extracted.get());
        long matched = checks.stream().filter(FieldCheck::matched).count();
        String status = matched == checks.size() ? MATCHED : MISMATCHED;

        ClmDocumentVerification saved = verificationRepository.save(new ClmDocumentVerification(
                documentId, status, checks.size(), (int) matched,
                writeJson(checks), excerpt, archived.fileType(), "UPSTAGE_DOCUMENT_AI"));
        return toResponse(saved);
    }

    /** 항목 하나의 대조 결과. */
    public record FieldCheck(String label, String expected, String found, boolean matched) {}

    private List<FieldCheck> compare(ClmCommitmentRepository.GratitudeContext source,
                                     String fallbackName,
                                     UpstageDocumentClient.ExtractedPledge found) {
        List<FieldCheck> checks = new ArrayList<>();

        String expectedName = source.applicantName() != null && !source.applicantName().isBlank()
                ? source.applicantName() : fallbackName;
        checks.add(text("약정자", expectedName, found.applicantName()));
        checks.add(text("수혜기관", source.organizationName(), found.organizationName()));

        if (source.pledgeAmount() != null && source.pledgeAmount().signum() > 0) {
            checks.add(amount("약정 금액", source.pledgeAmount(), found.pledgeAmount()));
        }
        if (source.pledgeFrequency() != null && !source.pledgeFrequency().isBlank()) {
            checks.add(frequency("약정 주기", source.pledgeFrequency(), found.pledgeFrequency()));
        }
        return checks;
    }

    private FieldCheck text(String label, String expected, String found) {
        return new FieldCheck(label, expected, found, normalize(expected).equals(normalize(found)));
    }

    /** "100,000 원"처럼 적힌 값도 같은 금액으로 본다. */
    private FieldCheck amount(String label, BigDecimal expected, String found) {
        String digits = found == null ? "" : found.replaceAll("[^0-9]", "");
        boolean same = !digits.isEmpty()
                && new BigDecimal(digits).compareTo(expected.setScale(0, java.math.RoundingMode.DOWN)) == 0;
        return new FieldCheck(label, expected.toBigInteger() + "원",
                found == null ? null : found + "원", same);
    }

    /** 약정서에는 MONTHLY가 아니라 "매월 정기"로 적힌다. 뜻이 같으면 일치로 본다. */
    private FieldCheck frequency(String label, String expectedCode, String found) {
        String expectedLabel = switch (expectedCode) {
            case "ONE_TIME" -> "일시";
            case "MONTHLY" -> "매월";
            case "QUARTERLY" -> "분기";
            case "ANNUAL" -> "매년";
            default -> expectedCode;
        };
        String normalizedFound = normalize(found);
        boolean same = !normalizedFound.isEmpty()
                && (normalizedFound.contains(normalize(expectedLabel))
                    || normalize(expectedCode).equals(normalizedFound));
        return new FieldCheck(label, expectedLabel, found, same);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }

    private String writeJson(List<FieldCheck> checks) {
        try {
            return objectMapper.writeValueAsString(checks);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ClmVerificationResponse toResponse(ClmDocumentVerification entity) {
        List<FieldCheck> checks;
        try {
            checks = objectMapper.readValue(
                    entity.getDetailJson() == null ? "[]" : entity.getDetailJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FieldCheck.class));
        } catch (Exception e) {
            checks = List.of();
        }
        return new ClmVerificationResponse(
                entity.getId(), entity.getClmDocumentId(), entity.getStatus(),
                entity.getCheckedCount(), entity.getMatchedCount(), checks,
                entity.getParsedExcerpt(), entity.getSourceFileType(), entity.getProvider(),
                entity.getCreatedAt());
    }
}
