package com.pixelcare.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.dto.AiRecommendResponse;
import com.pixelcare.domain.ai.dto.PledgeIntent;
import com.pixelcare.domain.ai.dto.RecommendedCardDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class UpstageApiClient {

    private static final Set<String> PLEDGE_TYPES = Set.of(
            "DONATION", "HOMETOWN_DONATION", "VOLUNTEER", "LEGACY_DONATION", "CULTURAL_HERITAGE_DONATION");
    private static final Set<String> FREQUENCIES = Set.of("ONE_TIME", "MONTHLY", "ANNUAL", "NOT_APPLICABLE");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${upstage.api.key}")
    private String apiKey;

    @Value("${upstage.api.base-url:https://api.upstage.ai/v1}")
    private String baseUrl;

    @Value("${upstage.api.model:solar-pro3}")
    private String model = "solar-pro3";

    public UpstageApiClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public record StructuredIntent(PledgeIntent intent, String source) {}

    public Optional<StructuredIntent> structurePledgeIntent(String userInput, String previousIntentJson) {
        if (apiKey == null || apiKey.contains("your_upstage") || apiKey.isBlank()) return Optional.empty();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String systemPrompt = """
                    사용자의 기부·봉사 약정 의사를 아래 JSON 스키마로만 정리하는 계약 정보 추출기다.
                    설명, 마크다운, 추측을 추가하지 말고 반드시 JSON 객체 하나만 출력하라.
                    이전 구조화 결과가 있으면 유지하되 현재 사용자 문장으로 명시적으로 수정된 값만 갱신하라.
                    모르는 값은 null로 두고 missingFields에는 필수 누락 필드명을 넣어라.
                    pledgeType: DONATION|HOMETOWN_DONATION|VOLUNTEER|LEGACY_DONATION|CULTURAL_HERITAGE_DONATION
                    frequency: ONE_TIME|MONTHLY|ANNUAL|NOT_APPLICABLE
                    startDate: YYYY-MM-DD, amount: 원 단위 양수 숫자
                    필드: pledgeType, beneficiary, amount, frequency, startDate, region, rewardPreference,
                    taxDeductionConsent, privacyConsent, specialConditions, missingFields
                    """;
            String previous = previousIntentJson == null || previousIntentJson.isBlank() ? "없음" : previousIntentJson;
            String userPrompt = "이전 구조화 약정 JSON:\n" + previous + "\n\n현재 사용자 문장:\n" + userInput;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return Optional.empty();

            JsonNode choices = objectMapper.readTree(response.getBody()).path("choices");
            if (!choices.isArray() || choices.isEmpty()) return Optional.empty();
            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) return Optional.empty();
            PledgeIntent intent = objectMapper.readValue(stripCodeFence(content), PledgeIntent.class);
            if (!hasValidEnums(intent)) return Optional.empty();
            return Optional.of(new StructuredIntent(intent, "UPSTAGE_SOLAR"));
        } catch (Exception e) {
            System.err.println("Upstage 약정 구조화 오류 (로컬 폴백 전환): " + e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * 전자서명이 끝난 약정을 사람이 건넨 한마디처럼 되돌려주는 감사 메시지를 만든다.
     * 키가 없거나 호출이 실패하면 비어 있는 값을 돌려주고, 호출부가 로컬 문구로 폴백한다.
     */
    public Optional<String> generateGratitudeMessage(String pledgeSummary) {
        if (apiKey == null || apiKey.contains("your_upstage") || apiKey.isBlank()) return Optional.empty();
        if (pledgeSummary == null || pledgeSummary.isBlank()) return Optional.empty();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String systemPrompt = """
                    너는 기부·봉사 약정에 전자서명을 마친 사람에게 건네는 짧은 감사 인사를 쓴다.
                    조건:
                    - 2문장 이내, 120자 이내의 한국어 존댓말.
                    - 약정 내용이 어떤 도움으로 이어지는지 구체적인 장면 하나를 담아라.
                    - 주어진 약정 정보에 없는 수치나 기관, 성과를 지어내지 마라.
                    - 이모지, 마크다운, 따옴표, 머리말 없이 인사말 본문만 출력하라.
                    """;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", "약정 정보:\n" + pledgeSummary)
            ));

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return Optional.empty();

            JsonNode choices = objectMapper.readTree(response.getBody()).path("choices");
            if (!choices.isArray() || choices.isEmpty()) return Optional.empty();
            String content = choices.get(0).path("message").path("content").asText("").trim();
            if (content.isBlank()) return Optional.empty();
            return Optional.of(content.length() > 300 ? content.substring(0, 300) : content);
        } catch (Exception e) {
            System.err.println("Upstage 감사 메시지 생성 오류 (로컬 폴백 전환): " + e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstLineEnd = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) return trimmed;
        return trimmed.substring(firstLineEnd + 1, closingFence).trim();
    }

    private boolean hasValidEnums(PledgeIntent intent) {
        return intent != null
                && (intent.pledgeType() == null || PLEDGE_TYPES.contains(intent.pledgeType()))
                && (intent.frequency() == null || FREQUENCIES.contains(intent.frequency()))
                && (intent.amount() == null || intent.amount().signum() > 0);
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
            requestBody.put("model", model);

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
            return "너는 잇다(ITDA)의 따뜻한 AI 메이트 'ITDA AI Mate'야.\n" +
                   "사용자에게 다정하고 친근한 말투로 봉사 및 기부를 안내해줘.\n" +
                   "아래는 우리 DB에서 사용자 요청과 매칭된 실제 봉사/기부 데이터야:\n\n" +
                   dbContextText + "\n\n" +
                   "반드시 위 목록에 있는 항목만 추천해. 목록에 없는 봉사나 기관은 절대 지어내지 마.\n" +
                   "출력 규칙(반드시 지켜라):\n" +
                   "- 표, 마크다운 문법(**, |, #, - 등), HTML 태그(<br> 등)를 쓰지 마라.\n" +
                   "- 항목을 나열하지 마라. 상세 정보는 화면의 추천 카드가 따로 보여준다.\n" +
                   "- 평범한 문장 2~3개로만 답하고, 마지막에 아래 추천 카드에서 상세 보기를 눌러보라고 안내해라.";
        } else {
            return "너는 잇다(ITDA)의 따뜻한 레트로 픽셀 AI 마스코트 'ITDA AI Mate'야.\n" +
                   "8-bit/16-bit 감성으로 사용자에게 다정하고 친근하게 대화해줘.\n" +
                   "사용자가 '안녕', '반가워', '너 누구야' 같은 인사나 소소한 대화를 걸면 밝고 따뜻하게 인사를 나눠줘.\n" +
                   "만약 사용자가 특정 봉사나 기부를 찾으려 했으나 DB 매칭이 없었던 경우라면, '현재 조건에 맞는 봉사는 등록되어 있지 않지만' 이라고 솔직히 말하고 유기견, 도시락, 학습지도, 플로깅 등 다른 추천 키워드를 친절히 제시해줘.";
        }
    }

    /**
     * 사용자의 입력이 단순 인사말이나 스몰토크인지 감지
     */
    private boolean isGreetingOrChitchat(String input) {
        if (input == null) return false;
        String clean = input.trim().replaceAll("[^가-힣a-zA-Z]", "");
        return clean.equals("안녕") || clean.equals("안녕하세요") || clean.equals("반가워") ||
               clean.equals("반가워요") || clean.equals("하이") || clean.equals("hello") ||
               clean.equals("hi") || clean.contains("너누구") || clean.contains("누구냐") ||
               clean.equals("고마워") || clean.equals("감사합니다") || clean.equals("수고해");
    }

    /**
     * Smart Failover: API 키 없거나 통신 장애 시에도 100% 동작하는 폴백
     * - 인사말/스몰토크 → 친근한 픽셀 AI 대화
     * - DB 매칭 있음 → 카드 + 안내 메시지
     * - DB 매칭 없음 → 없음 안내 (카드 없음)
     */
    private AiRecommendResponse getFallbackResponse(String userInput, List<RecommendedCardDto> dbCandidateCards, boolean hasMatches) {
        if (!hasMatches) {
            // 인사말이나 단순 스몰토크인 경우
            if (isGreetingOrChitchat(userInput)) {
                String reply = "안녕! 나는 잇다의 든든한 AI 메이트야 👾✨\n" +
                        "오늘 어떤 봉사활동이나 기부처를 찾고 있니?\n" +
                        "부산 지역의 유기견 봉사, 도시락 배달, 학습 지도 등 궁금한 점이 있다면 언제든 편하게 물어봐줘! 😊";
                return new AiRecommendResponse(reply, List.of());
            }

            // 구체적 봉사/기부 검색어였으나 DB 매칭 결과 없음
            String reply = String.format(
                "안녕! 나는 픽셀 AI Mate야 👾✨\n" +
                "**'%s'** 관련 봉사/기부를 찾아봤는데, 현재 우리 DB에 등록된 항목 중 해당 조건과 딱 맞는 게 없어요 😥\n\n" +
                "아래 키워드로 다시 물어봐주면 더 정확하게 찾아줄 수 있어!\n" +
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
            "아래 추천 카드의 **[상세 보기]**를 클릭하면 상세 정보와 위치를 바로 확인할 수 있어! 💪",
            userInput,
            topCards.size(),
            cardTitles
        );

        return new AiRecommendResponse(reply, topCards);
    }
}
