package com.pixelcare.domain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.service.UpstageApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UpstageApiClientStructuredIntentTest {

    @Test
    void sendsConsentedContextAndParsesStructuredIntent() throws Exception {
        UpstageApiClient client = new UpstageApiClient();
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "baseUrl", "https://api.upstage.test/v1/solar");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String intentJson = """
                {"pledgeType":"HOMETOWN_DONATION","beneficiary":"부산 지역 아동","amount":30000,
                "frequency":"MONTHLY","startDate":"2026-08-01","region":"부산","rewardPreference":"NONE",
                "taxDeductionConsent":null,"privacyConsent":null,"specialConditions":"답례품 불필요","missingFields":[]}
                """;
        String response = new ObjectMapper().writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", "```json\n" + intentJson + "\n```")))
        ));

        server.expect(requestTo("https://api.upstage.test/v1/solar/chat/completions"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(allOf(
                        containsString("매월 3만원"),
                        containsString("\\\"pledgeType\\\":\\\"DONATION\\\"")
                )))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        var result = client.structurePledgeIntent(
                "부산 아동에게 매월 3만원을 기부하고 답례품은 필요 없어요.",
                "{\"pledgeType\":\"DONATION\"}"
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().source()).isEqualTo("UPSTAGE_SOLAR");
        assertThat(result.orElseThrow().intent().pledgeType()).isEqualTo("HOMETOWN_DONATION");
        assertThat(result.orElseThrow().intent().amount()).isEqualByComparingTo("30000");
        server.verify();
    }

    @Test
    void doesNotCallUpstageWhenApiKeyIsMissing() {
        UpstageApiClient client = new UpstageApiClient();
        ReflectionTestUtils.setField(client, "apiKey", "your_upstage_api_key");

        assertThat(client.structurePledgeIntent("기부하고 싶어요", null)).isEmpty();
    }
}
