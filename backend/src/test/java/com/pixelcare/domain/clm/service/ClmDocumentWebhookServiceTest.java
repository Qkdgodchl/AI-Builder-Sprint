package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmCommitmentRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.domain.clm.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClmDocumentWebhookServiceTest {

    @Mock ClmDocumentRepository documentRepository;
    @Mock ClmCommitmentRepository commitmentRepository;
    @Mock WebhookEventRepository webhookEventRepository;
    @Mock ModusignApiClient modusignApiClient;
    @Mock ClmDocumentArchiveService archiveService;

    private ClmDocumentService service;

    @BeforeEach
    void setUp() {
        service = new ClmDocumentService(documentRepository, commitmentRepository,
                webhookEventRepository, modusignApiClient, archiveService);
    }

    @Test
    void duplicateWebhookStopsBeforeApplyingStateAgain() {
        when(webhookEventRepository.start("MODUSIGN", "event-1", "document_all_signed", "{}"))
                .thenReturn(false);

        service.applyWebhookEvent("event-1", "document-1", "document_all_signed", "{}");

        verifyNoInteractions(documentRepository, commitmentRepository, archiveService);
        verify(webhookEventRepository, never()).complete(anyString(), anyString());
    }

    @Test
    void archiveFailureMarksWebhookAsFailedAndPropagatesError() {
        ClmDocument document = document();
        when(webhookEventRepository.start("MODUSIGN", "event-2", "document_all_signed", "{}"))
                .thenReturn(true);
        when(documentRepository.findByModusignDocumentIdAndIsDeletedFalse("document-2"))
                .thenReturn(Optional.of(document));
        doThrow(new IllegalStateException("archive failed"))
                .when(archiveService).archiveCompletedFiles(document);

        assertThatThrownBy(() -> service.applyWebhookEvent(
                "event-2", "document-2", "document_all_signed", "{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("archive failed");

        verify(webhookEventRepository).fail("MODUSIGN", "event-2", "archive failed");
        verify(webhookEventRepository, never()).complete(anyString(), anyString());
    }

    private ClmDocument document() {
        return new ClmDocument(
                1L, 2L, 3L, "부산 고향사랑 정기기부 약정", 4L,
                "홍길동", "user@example.com", null,
                "document-2", "participant-1", "template-1",
                "https://sign.example.com", LocalDateTime.now().plusMinutes(10)
        );
    }
}
