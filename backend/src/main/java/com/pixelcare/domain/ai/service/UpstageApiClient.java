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
     * - DB 매칭 결과가 없으면 카드를 만들지 않고 "없음" 안내
     * - DB 매칭 결과가 있으면 해당 카드만 반환
     */
    public AiRecommendResponse generateRecommendation(String userInput, String dbContextText, List<RecommendedCardDto> dbCandidateCards) {
        boolean hasMatches = !dbCandidateCards.isEmpty();

        // API 키가 없거나 테스트 가짜 키일 경우 폴백 반환
        if (apiKey == null || apiKey.contains("your_upstage") || apiKey.trim().isEmpty()) {
            return getFallbackResponse(userInput, dbCandidateCards, hasMatches);
        }

        try {
            String systemPrompt = buildSystemPrompt(dbContextText, hasMatches);

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

                // DB에 매칭 결과 있으면 최대 3개 카드 반환, 없으면 빈 카드
                List<RecommendedCardDto> returnCards = hasMatches
                        ? dbCandidateCards.subList(0, Math.min(3, dbCandidateCards.size()))
                        : List.of();

                return new AiRecommendResponse(aiReply, returnCards);
            }
        } catch (Exception e) {
            System.err.println("Upstage Solar API 호출 오류 (Fallback 전환): " + e.getMessage());
        }

        return getFallbackResponse(userInput, dbCandidateCards, hasMatches);
    }

    private String buildSystemPrompt(String dbContextText, boolean hasMatches) {
        if (hasMatches) {
            return "너는 픽셀 케어(Pixel Care)의 따뜻한 레트로 픽셀 AI 마스코트 'Pixel AI Mate'야.\n" +
                   "8-bit/16-bit 감성으로 사용자에게 다정하고 친근하게 봉사 및 기부를 추천해줘.\n" +
                   "아래는 우리 DB에서 사용자 요청과 매칭된 실제 봉사/기부 데이터야:\n\n" +
                   dbContextText + "\n\n" +
                   "반드시 위 목록에 있는 항목만 추천해. 목록에 없는 봉사나 기관은 절대 지어내지 마.\n" +
                   "추천 카드가 아래에 표시될 예정이니 '아래 카드를 클릭해 상세정보를 확인해보세요!' 라고 안내해줘.";
        } else {
            return "너는 픽셀 케어(Pixel Care)의 따뜻한 레트로 픽셀 AI 마스코트 'Pixel AI Mate'야.\n" +
                   "8-bit/16-bit 감성으로 사용자에게 다정하고 친근하게 대화해줘.\n" +
                   "현재 우리 DB에 사용자가 요청한 조건과 일치하는 봉사/기부 항목이 없어.\n" +
                   "절대로 없는 봉사를 지어내지 마. 솔직하게 '현재 등록된 봉사/기부 중에 해당 조건과 맞는 항목이 없어요' 라고 안내해줘.\n" +
                   "대신 어떤 다른 키워드(유기견, 도시락, 학습지도, 플로깅, 노인, 아동 등)로 검색하면 좋을지 친절하게 알려줘.";
        }
    }

    /**
     * Smart Failover: API 키 없거나 통신 장애 시에도 100% 동작하는 폴백
     * - DB 매칭 있으면 → 카드 + 안내 메시지
     * - DB 매칭 없으면 → 없음 안내 (카드 없음)
     */
    private AiRecommendResponse getFallbackResponse(String userInput, List<RecommendedCardDto> dbCandidateCards, boolean hasMatches) {
        if (!hasMatches) {
            // DB에 해당 항목 없음 → 명확히 "없음" 안내
            String reply = String.format(
                "안녕! 나는 픽셀 AI Mate야 👾✨\n" +
                "**'%s'** 관련 봉사/기부를 찾아봤는데, 현재 우리 DB에 등록된 항목 중 해당 조건과 맞는 게 없어요 😥\n\n" +
                "아래 키워드로 다시 물어봐줘!\n" +
                "🐕 유기견 봉사  |  🍲 도시락 배달  |  📚 학습 지도\n" +
                "🌿 플로깅  |  👴 노인 말벗  |  ❤️ 기부 후원",
                userInput
            );
            return new AiRecommendResponse(reply, List.of());
        }

        // DB에 매칭 결과 있음 → 최대 3개 카드 + 안내 메시지
        List<RecommendedCardDto> topCards = dbCandidateCards.subList(0, Math.min(3, dbCandidateCards.size()));
        String cardTitles = topCards.stream()
                .map(c -> "**" + c.getTitle() + "**")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        String reply = String.format(
            "안녕! 나는 픽셀 AI Mate야 👾✨\n" +
            "'%s' 관련해서 우리 DB에서 %d개의 딱 맞는 항목을 찾았어! 🎯\n" +
            "%s\n\n" +
            "아래 추천 카드를 클릭하면 상세 정보와 신청 방법을 바로 확인할 수 있어! 💪",
            userInput,
            topCards.size(),
            cardTitles
        );

        return new AiRecommendResponse(reply, topCards);
    }
}
