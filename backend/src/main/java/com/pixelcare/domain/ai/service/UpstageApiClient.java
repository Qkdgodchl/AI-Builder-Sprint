package com.pixelcare.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.dto.AiRecommendResponse;
import com.pixelcare.domain.ai.dto.RecommendedCardDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpstageApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${upstage.api.key}")
    private String apiKey;

    @Value("${upstage.api.base-url:https://api.upstage.ai/v1/solar}")
    private String baseUrl;

    public UpstageApiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Upstage Solar LLM을 호출하여 내부 DB 데이터 기반 챗봇 답변을 받습니다.
     */
    public AiRecommendResponse generateRecommendation(String userInput, String dbContextText, List<RecommendedCardDto> dbCandidateCards) {
        // API 키가 없거나 테스트 가짜 키일 경우 폴백 반환
        if (apiKey == null || apiKey.contains("your_upstage") || apiKey.trim().isEmpty()) {
            return getFallbackResponse(userInput, dbCandidateCards);
        }

        try {
            String systemPrompt = "너는 픽셀 케어(Pixel Care)의 따뜻한 레트로 픽셀 AI 마스코트 'Pixel AI Mate'야.\n" +
                    "8-bit/16-bit 감성으로 사용자에게 다정하고 친근하게 봉사 및 기부를 추천해줘.\n" +
                    "아래는 우리 DB에 등록된 실제 봉사/기부 데이터 목록이야:\n" +
                    dbContextText + "\n\n" +
                    "위 DB 목록에 있는 정보에 기반해서만 사용자의 질문에 답해줘. 없는 봉사는 지어내지 마.";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "solar-mini");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userInput));
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String aiReply = rootNode.path("choices").get(0).path("message").path("content").asText();

                return new AiRecommendResponse(aiReply, dbCandidateCards);
            }
        } catch (Exception e) {
            // 통신 장애 시 Smart Failover 처리
            System.err.println("Upstage Solar API 호출 오류 (Fallback 전환): " + e.getMessage());
        }

        return getFallbackResponse(userInput, dbCandidateCards);
    }

    /**
     * Smart Failover: 외부 장애 시에도 멈추지 않는 폴백 로직
     */
    private AiRecommendResponse getFallbackResponse(String userInput, List<RecommendedCardDto> dbCandidateCards) {
        String fallbackReply = "안녕하세요! 픽셀 마스코트 AI Mate예요 👾✨ '" + userInput + "'에 딱 맞는 온기 넘치는 선행 미션을 우리 DB에서 찾아왔어요! 아래 카드를 확인해보세요!";
        return new AiRecommendResponse(fallbackReply, dbCandidateCards);
    }
}
