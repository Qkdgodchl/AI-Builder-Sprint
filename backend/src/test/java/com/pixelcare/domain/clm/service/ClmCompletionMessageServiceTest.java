package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.ai.service.UpstageApiClient;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmCommitmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClmCompletionMessageServiceTest {

    @Mock ClmCommitmentRepository commitmentRepository;
    @Mock UpstageApiClient upstageApiClient;

    private ClmCompletionMessageService service;

    @BeforeEach
    void setUp() {
        service = new ClmCompletionMessageService(commitmentRepository, upstageApiClient);
    }

    @Test
    void solarMessageIsStoredWithItsSource() {
        ClmDocument document = signedDocument();
        when(commitmentRepository.findGratitudeContext(1L)).thenReturn(Optional.of(context()));
        when(upstageApiClient.generateGratitudeMessage(anyString()))
                .thenReturn(Optional.of("홍길동님의 정기 후원이 아이들의 한 달 식탁을 지킵니다."));

        service.attachTo(document);

        assertThat(document.getCompletionMessage())
                .isEqualTo("홍길동님의 정기 후원이 아이들의 한 달 식탁을 지킵니다.");
        assertThat(document.getCompletionMessageSource()).isEqualTo("UPSTAGE_SOLAR");
        assertThat(document.getCompletionMessageCreatedAt()).isNotNull();
    }

    @Test
    void localFallbackKeepsTheMessageWhenSolarIsUnavailable() {
        ClmDocument document = signedDocument();
        when(commitmentRepository.findGratitudeContext(1L)).thenReturn(Optional.of(context()));
        when(upstageApiClient.generateGratitudeMessage(anyString())).thenReturn(Optional.empty());

        service.attachTo(document);

        assertThat(document.getCompletionMessageSource()).isEqualTo("LOCAL_FALLBACK");
        assertThat(document.getCompletionMessage())
                .contains("홍길동")
                .contains("부산 아동복지 센터")
                .contains("50,000원");
    }

    @Test
    void secondCallDoesNotOverwriteOrCallSolarAgain() {
        ClmDocument document = signedDocument();
        when(commitmentRepository.findGratitudeContext(1L)).thenReturn(Optional.of(context()));
        when(upstageApiClient.generateGratitudeMessage(anyString()))
                .thenReturn(Optional.of("첫 번째 인사"));
        service.attachTo(document);
        clearInvocations(upstageApiClient, commitmentRepository);

        service.attachTo(document);

        assertThat(document.getCompletionMessage()).isEqualTo("첫 번째 인사");
        verifyNoInteractions(upstageApiClient, commitmentRepository);
    }

    @Test
    void unsignedDocumentKeepsNoMessage() {
        ClmDocument document = document();
        when(commitmentRepository.findGratitudeContext(1L)).thenReturn(Optional.of(context()));
        when(upstageApiClient.generateGratitudeMessage(anyString()))
                .thenReturn(Optional.of("아직 서명 전인데 남은 인사"));

        service.attachTo(document);

        assertThat(document.hasCompletionMessage()).isFalse();
    }

    @Test
    void missingCommitmentIsSkippedQuietly() {
        ClmDocument document = signedDocument();
        when(commitmentRepository.findGratitudeContext(1L)).thenReturn(Optional.empty());

        service.attachTo(document);

        assertThat(document.hasCompletionMessage()).isFalse();
        verifyNoInteractions(upstageApiClient);
    }

    private ClmCommitmentRepository.GratitudeContext context() {
        return new ClmCommitmentRepository.GratitudeContext(
                "홍길동", "부산 아동 정기후원 약정", "DONATION",
                new BigDecimal("50000"), "MONTHLY", "부산 아동복지 센터"
        );
    }

    private ClmDocument signedDocument() {
        ClmDocument document = document();
        document.applyModusignEvent("document_all_signed");
        return document;
    }

    private ClmDocument document() {
        return new ClmDocument(
                1L, 2L, 3L, "부산 아동 정기후원 약정", 4L,
                "홍길동", "user@example.com", null,
                "document-1", "participant-1", "template-1",
                "https://sign.example.com", LocalDateTime.now().plusMinutes(10)
        );
    }
}
