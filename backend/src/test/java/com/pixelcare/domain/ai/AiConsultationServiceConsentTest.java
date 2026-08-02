package com.pixelcare.domain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.dto.PledgeIntent;
import com.pixelcare.domain.ai.repository.AiConsultationRepository;
import com.pixelcare.domain.ai.service.AiConsultationService;
import com.pixelcare.domain.ai.service.UpstageApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiConsultationServiceConsentTest {

    @Mock
    private AiConsultationRepository repository;
    @Mock
    private UpstageApiClient upstageApiClient;

    private AiConsultationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new AiConsultationService(repository, upstageApiClient, objectMapper);
        when(repository.create(10L)).thenReturn(41L);
        when(repository.findOwned(41L, 10L)).thenReturn(Optional.of(storedRow()));
    }

    @Test
    void usesLocalFallbackWithoutExternalConsent() {
        var response = service.start(10L, "부산에서 봉사하고 싶어요", false);

        assertThat(response.source()).isEqualTo("RULE_FALLBACK");
        verify(repository, never()).recordExternalAiConsent(41L, 10L);
        verifyNoInteractions(upstageApiClient);
    }

    @Test
    void recordsConsentBeforeCallingUpstage() {
        PledgeIntent intent = new PledgeIntent(
                "DONATION", "부산 지역 아동", BigDecimal.valueOf(30_000), "MONTHLY",
                LocalDate.of(2026, 8, 1), "부산", null, null, null, null,
                null, null, null, null, null, List.of());
        when(upstageApiClient.structurePledgeIntent("매월 3만원을 기부하고 싶어요", null))
                .thenReturn(Optional.of(new UpstageApiClient.StructuredIntent(intent, "UPSTAGE_SOLAR")));

        var response = service.start(10L, "매월 3만원을 기부하고 싶어요", true);

        assertThat(response.source()).isEqualTo("UPSTAGE_SOLAR");
        verify(repository).recordExternalAiConsent(41L, 10L);
        verify(upstageApiClient).structurePledgeIntent("매월 3만원을 기부하고 싶어요", null);
    }

    private Map<String, Object> storedRow() {
        PledgeIntent stored = new PledgeIntent(
                "DONATION", "부산 지역 아동", BigDecimal.valueOf(30_000), "MONTHLY",
                LocalDate.of(2026, 8, 1), "부산", null, null, null, null,
                null, null, null, null, null, List.of());
        Map<String, Object> row = new HashMap<>();
        row.put("id", 41L);
        row.put("consultation_status", "IN_PROGRESS");
        row.put("intent_summary", "부산 지역 아동에 매월 30000원 약정");
        try {
            row.put("extracted_preferences_json", objectMapper.writeValueAsString(stored));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return row;
    }
}
