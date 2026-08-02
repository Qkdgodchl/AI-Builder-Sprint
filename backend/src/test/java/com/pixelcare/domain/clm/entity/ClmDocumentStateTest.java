package com.pixelcare.domain.clm.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClmDocumentStateTest {

    @Test
    void terminalSignedStateCannotRegressWhenAnOlderEventArrives() {
        ClmDocument document = document();

        assertThat(document.applyModusignEvent("document_all_signed")).isTrue();
        LocalDateTime signedAt = document.getSignedAt();

        assertThat(document.applyModusignEvent("document_started")).isFalse();
        assertThat(document.getStatus()).isEqualTo("SIGNED");
        assertThat(document.getSignedAt()).isEqualTo(signedAt);
        assertThat(document.getLastEventType()).isEqualTo("document_all_signed");
    }

    @Test
    void duplicateTerminalEventIsIgnored() {
        ClmDocument document = document();
        document.applyModusignEvent("document_all_signed");

        assertThat(document.applyModusignEvent("document_all_signed")).isFalse();
        assertThat(document.getLastEventRank()).isEqualTo(100);
    }

    private ClmDocument document() {
        return new ClmDocument(
                1L, 2L, 3L, "부산 고향사랑 정기기부 약정", 4L,
                "홍길동", "user@example.com", null,
                "modu-1", "participant-1", "template-1",
                "https://sign.example.com", LocalDateTime.now().plusMinutes(10)
        );
    }
}
