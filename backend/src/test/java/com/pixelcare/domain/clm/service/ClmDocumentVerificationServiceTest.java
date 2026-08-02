package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.ai.service.UpstageDocumentClient;
import com.pixelcare.domain.clm.dto.ClmVerificationResponse;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.entity.ClmDocumentVerification;
import com.pixelcare.domain.clm.repository.ClmCommitmentRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentVerificationRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ClmDocumentVerificationServiceTest {

    private final ClmDocumentAccessService accessService = mock(ClmDocumentAccessService.class);
    private final ClmDocumentArchiveService archiveService = mock(ClmDocumentArchiveService.class);
    private final ClmCommitmentRepository commitmentRepository = mock(ClmCommitmentRepository.class);
    private final ClmDocumentVerificationRepository verificationRepository =
            mock(ClmDocumentVerificationRepository.class);
    private final UpstageDocumentClient documentClient = mock(UpstageDocumentClient.class);

    private final ClmDocumentVerificationService service = new ClmDocumentVerificationService(
            accessService, archiveService, commitmentRepository, verificationRepository, documentClient);

    private final CurrentUser user = new CurrentUser(1L, "donor@pixelcare.demo", "데모 후원자", Set.of("USER"));

    private void givenArchivedContract(BigDecimal amount, String frequency) {
        ClmDocument document = new ClmDocument(
                10L, 20L, 7L, "정기후원 약정", 1L, "전동훈", "donor@pixelcare.demo", null,
                "MODU-1", "PART-1", "PDF_UPLOAD", "https://app.modusign.co.kr/x", LocalDateTime.now());
        when(accessService.requireAccess(anyLong(), any())).thenReturn(document);
        when(documentClient.isConfigured()).thenReturn(true);
        when(archiveService.readForVerification(anyLong())).thenReturn(Optional.of(
                new ClmDocumentArchiveService.ArchivedPdf(
                        "pdf".getBytes(StandardCharsets.UTF_8), "signed.pdf", "SIGNED_DOCUMENT")));
        when(commitmentRepository.findGratitudeContext(anyLong())).thenReturn(Optional.of(
                new ClmCommitmentRepository.GratitudeContext(
                        "전동훈", "정기후원 약정", "DONATION", amount, frequency, "잇다 데모 센터")));
        when(documentClient.parseText(any(), any())).thenReturn(Optional.of("약정서 본문"));
        when(verificationRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void 서명본과_약정_값이_같으면_일치로_본다() {
        givenArchivedContract(new BigDecimal("30000"), "MONTHLY");
        // 약정서에는 "30,000 원", "매월 정기 후원"처럼 사람이 읽는 형태로 적힌다.
        when(documentClient.extractPledge(any())).thenReturn(Optional.of(
                new UpstageDocumentClient.ExtractedPledge(
                        "전동훈", "잇다 데모 센터", "30,000", "매월 정기 후원", "2026-08-03")));

        ClmVerificationResponse result = service.verify(1L, user);

        assertThat(result.status()).isEqualTo("MATCHED");
        assertThat(result.checkedCount()).isEqualTo(4);
        assertThat(result.matchedCount()).isEqualTo(4);
        assertThat(result.checks()).allMatch(ClmDocumentVerificationService.FieldCheck::matched);
    }

    @Test
    void 금액이_다르면_어떤_항목이_어긋났는지_남긴다() {
        givenArchivedContract(new BigDecimal("30000"), "MONTHLY");
        when(documentClient.extractPledge(any())).thenReturn(Optional.of(
                new UpstageDocumentClient.ExtractedPledge(
                        "전동훈", "잇다 데모 센터", "50,000", "매월 정기 후원", "2026-08-03")));

        ClmVerificationResponse result = service.verify(1L, user);

        assertThat(result.status()).isEqualTo("MISMATCHED");
        assertThat(result.matchedCount()).isEqualTo(3);
        assertThat(result.checks())
                .filteredOn(check -> !check.matched())
                .singleElement()
                .satisfies(check -> {
                    assertThat(check.label()).isEqualTo("약정 금액");
                    assertThat(check.expected()).isEqualTo("30000원");
                });
    }

    @Test
    void 약정서에_적힌_주기_표기를_같은_주기로_읽는다() {
        // 실제 체결본에는 ANNUAL이 "연간 정기 후원"으로 적힌다.
        givenArchivedContract(new BigDecimal("120000"), "ANNUAL");
        when(documentClient.extractPledge(any())).thenReturn(Optional.of(
                new UpstageDocumentClient.ExtractedPledge(
                        "전동훈", "잇다 데모 센터", "120000", "연간 정기 후원", "2026-08-03")));

        ClmVerificationResponse result = service.verify(1L, user);

        assertThat(result.status()).isEqualTo("MATCHED");
        assertThat(result.checks())
                .filteredOn(check -> check.label().equals("약정 주기"))
                .singleElement()
                .satisfies(check -> assertThat(check.matched()).isTrue());
    }

    @Test
    void 문서를_읽지_못하면_결과를_지어내지_않는다() {
        givenArchivedContract(new BigDecimal("30000"), "MONTHLY");
        when(documentClient.extractPledge(any())).thenReturn(Optional.empty());

        ClmVerificationResponse result = service.verify(1L, user);

        assertThat(result.status()).isEqualTo("UNREADABLE");
        assertThat(result.checks()).isEmpty();
        assertThat(result.matchedCount()).isZero();
    }

    @Test
    void 보관된_약정서가_없으면_검증하지_않는다() {
        ClmDocument document = new ClmDocument(
                10L, 20L, 7L, "정기후원 약정", 1L, "전동훈", "donor@pixelcare.demo", null,
                "MODU-1", "PART-1", "PDF_UPLOAD", "https://app.modusign.co.kr/x", LocalDateTime.now());
        when(accessService.requireAccess(anyLong(), any())).thenReturn(document);
        when(documentClient.isConfigured()).thenReturn(true);
        when(archiveService.readForVerification(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(1L, user))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("보관되지 않았습니다");
        verify(verificationRepository, never()).save(any(ClmDocumentVerification.class));
    }
}
