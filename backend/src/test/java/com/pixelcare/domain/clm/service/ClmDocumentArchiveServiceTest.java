package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.entity.ClmDocumentFile;
import com.pixelcare.domain.clm.repository.ClmDocumentFileRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClmDocumentArchiveServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesSignedDocumentAndAuditTrailWithHashes() throws Exception {
        ClmDocumentFileRepository fileRepository = mock(ClmDocumentFileRepository.class);
        ClmDocumentAccessService accessService = mock(ClmDocumentAccessService.class);
        ModusignApiClient apiClient = mock(ModusignApiClient.class);
        byte[] signedPdf = "signed-pdf".getBytes(StandardCharsets.UTF_8);
        byte[] auditPdf = "audit-pdf".getBytes(StandardCharsets.UTF_8);
        when(apiClient.downloadCompletedDocumentFiles("modusign-1"))
                .thenReturn(new ModusignApiClient.CompletedDocumentFiles(signedPdf, auditPdf));

        ClmDocument document = new ClmDocument(
                8L, "봉사 모집글", 1L, "홍길동", "user@example.com", null,
                "modusign-1", "participant-1", "template-1",
                "https://sign.example", LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(document, "id", 33L);

        ClmDocumentArchiveService service = new ClmDocumentArchiveService(
                fileRepository, accessService, apiClient, tempDir.toString()
        );
        service.archiveCompletedFiles(document);

        ArgumentCaptor<ClmDocumentFile> captor = ArgumentCaptor.forClass(ClmDocumentFile.class);
        verify(fileRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ClmDocumentFile::getFileType)
                .containsExactly("SIGNED_DOCUMENT", "AUDIT_TRAIL");
        assertThat(Files.list(tempDir.resolve("clm")).count()).isEqualTo(2);
        assertThat(captor.getAllValues())
                .allSatisfy(file -> assertThat(file.getSha256()).hasSize(64));
    }

    @Test
    void duplicateWebhookDoesNotDownloadAgain() {
        ClmDocumentFileRepository fileRepository = mock(ClmDocumentFileRepository.class);
        ClmDocumentAccessService accessService = mock(ClmDocumentAccessService.class);
        ModusignApiClient apiClient = mock(ModusignApiClient.class);
        // 같은 서명 세션(modusign-1)의 체결본이 이미 보관돼 있는 상황.
        when(fileRepository.findByClmDocumentIdAndIsDeletedFalseOrderByIdAsc(33L))
                .thenReturn(java.util.List.of(new ClmDocumentFile(
                        33L, "SIGNED_DOCUMENT", "key", "pixelcare-33-signed-document-modusign-1.pdf",
                        "application/pdf", 10L, "a".repeat(64), new byte[]{1})));
        ClmDocument document = new ClmDocument(
                8L, "봉사 모집글", 1L, "홍길동", "user@example.com", null,
                "modusign-1", "participant-1", "template-1",
                "https://sign.example", LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(document, "id", 33L);
        ClmDocumentArchiveService service = new ClmDocumentArchiveService(
                fileRepository, accessService, apiClient, tempDir.toString()
        );

        service.archiveCompletedFiles(document);

        verifyNoInteractions(apiClient);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void reSignedSessionArchivesAgainUnderNewDocumentId() {
        ClmDocumentFileRepository fileRepository = mock(ClmDocumentFileRepository.class);
        ClmDocumentAccessService accessService = mock(ClmDocumentAccessService.class);
        ModusignApiClient apiClient = mock(ModusignApiClient.class);
        // 첫 세션(modusign-1) 체결본만 보관된 상태에서 재서명 세션(modusign-2)이 완료됐다.
        when(fileRepository.findByClmDocumentIdAndIsDeletedFalseOrderByIdAsc(33L))
                .thenReturn(java.util.List.of(new ClmDocumentFile(
                        33L, "SIGNED_DOCUMENT", "key", "pixelcare-33-signed-document-modusign-1.pdf",
                        "application/pdf", 10L, "a".repeat(64), new byte[]{1})));
        when(apiClient.downloadCompletedDocumentFiles("modusign-2"))
                .thenReturn(new ModusignApiClient.CompletedDocumentFiles(
                        "signed-pdf-2".getBytes(StandardCharsets.UTF_8), null));
        ClmDocument document = new ClmDocument(
                8L, "봉사 모집글", 1L, "홍길동", "user@example.com", null,
                "modusign-2", "participant-2", "template-1",
                "https://sign.example", LocalDateTime.now().plusMinutes(10)
        );
        ReflectionTestUtils.setField(document, "id", 33L);
        ClmDocumentArchiveService service = new ClmDocumentArchiveService(
                fileRepository, accessService, apiClient, tempDir.toString()
        );

        service.archiveCompletedFiles(document);

        ArgumentCaptor<ClmDocumentFile> captor = ArgumentCaptor.forClass(ClmDocumentFile.class);
        verify(fileRepository).save(captor.capture());
        assertThat(captor.getValue().getOriginalName()).endsWith("signed-document-modusign-2.pdf");
    }
}
