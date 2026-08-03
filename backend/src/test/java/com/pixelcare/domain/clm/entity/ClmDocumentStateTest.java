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

    @Test
    void newSigningSessionResetsTerminalEventSoDocumentCanBeSignedAgain() {
        ClmDocument document = document();
        document.applyModusignEvent("document_all_signed");
        assertThat(document.getStatus()).isEqualTo("SIGNED");

        // 재서명 세션을 붙이면 이전 세션의 종결 이벤트(rank 100)가 지워져야
        // 새 서명 완료 이벤트가 다시 반영될 수 있다.
        document.updateSigningSession(9L, "modu-2", "participant-2",
                "https://sign.example.com/2", LocalDateTime.now().plusMinutes(10));
        assertThat(document.getStatus()).isEqualTo("PENDING_SIGNATURE");
        assertThat(document.getLastEventRank()).isEqualTo(0);
        assertThat(document.getSignedAt()).isNull();

        assertThat(document.applyModusignEvent("document_all_signed")).isTrue();
        assertThat(document.getStatus()).isEqualTo("SIGNED");
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
