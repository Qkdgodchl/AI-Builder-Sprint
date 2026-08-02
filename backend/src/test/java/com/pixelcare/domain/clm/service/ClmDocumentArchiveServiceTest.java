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
        when(fileRepository.existsByClmDocumentIdAndFileTypeAndIsDeletedFalse(33L, "SIGNED_DOCUMENT"))
                .thenReturn(true);
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
}
