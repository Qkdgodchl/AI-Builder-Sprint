package com.pixelcare.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.dto.AiRecommendRequest;
import com.pixelcare.domain.ai.dto.AiRecommendResponse;
import com.pixelcare.domain.ai.dto.ChatMessageResponse;
import com.pixelcare.domain.ai.dto.RecommendedCardDto;
import com.pixelcare.domain.ai.entity.ChatMessage;
import com.pixelcare.domain.ai.repository.ChatMessageRepository;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.repository.PostRepository;
import com.pixelcare.volunteer.Volunteer;
import com.pixelcare.volunteer.VolunteerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AiMateService {

    private final UpstageApiClient upstageApiClient;
    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final VolunteerRepository volunteerRepository;
    private final ObjectMapper objectMapper;

    /**
     * 지명 → 해당 구 및 인접 구 확장 매핑
     * 예) "광안리" 입력 시 수영구·해운대·남구까지 확장하여 검색
     */
    private static final Map<String, List<String>> LOCATION_ALIASES;
    static {
        LOCATION_ALIASES = new HashMap<>();
        // 해운대·광안리·수영 인접권 (가장 중요)
        LOCATION_ALIASES.put("해운대", List.of("해운대", "수영", "광안"));
        LOCATION_ALIASES.put("광안리", List.of("수영", "해운대", "남구", "동래구"));
        LOCATION_ALIASES.put("광안", List.of("수영", "해운대", "남구"));
        LOCATION_ALIASES.put("수영", List.of("수영구", "해운대", "남구"));
        LOCATION_ALIASES.put("민락", List.of("수영구", "해운대"));
        LOCATION_ALIASES.put("센텀", List.of("해운대"));
        LOCATION_ALIASES.put("마린시티", List.of("해운대"));
        // 부산대·금정 일대
        LOCATION_ALIASES.put("부산대", List.of("금정구", "장전", "사상구"));
        LOCATION_ALIASES.put("장전", List.of("금정구", "사상구"));
        LOCATION_ALIASES.put("노포", List.of("금정구"));
        LOCATION_ALIASES.put("금정", List.of("금정구", "범어사", "동래구"));
        LOCATION_ALIASES.put("범어사", List.of("금정구"));
        // 남구·대연·용호 일대
        LOCATION_ALIASES.put("대연", List.of("남구", "수영구", "해운대"));
        LOCATION_ALIASES.put("용호", List.of("남구", "수영구"));
        LOCATION_ALIASES.put("문현", List.of("남구", "수영구"));
        LOCATION_ALIASES.put("남구", List.of("남구", "수영구", "해운대"));
        // 서면·부산진 일대
        LOCATION_ALIASES.put("서면", List.of("부산진구", "연제구", "동래구"));
        LOCATION_ALIASES.put("전포", List.of("부산진구"));
        LOCATION_ALIASES.put("부전", List.of("부산진구"));
        // 남포·중구·영도·초량 일대
        LOCATION_ALIASES.put("남포", List.of("중구", "영도구"));
        LOCATION_ALIASES.put("영도", List.of("영도구", "중구"));
        LOCATION_ALIASES.put("초량", List.of("동구", "중구", "영도구"));
        LOCATION_ALIASES.put("부산역", List.of("동구", "중구"));
        // 동래·사직·연산 일대
        LOCATION_ALIASES.put("동래", List.of("동래구", "연제구", "금정구"));
        LOCATION_ALIASES.put("사직", List.of("동래구", "연제구"));
        LOCATION_ALIASES.put("연산", List.of("연제구", "동래구"));
        LOCATION_ALIASES.put("거제", List.of("연제구", "동래구"));
        // 북구·덕천 일대
        LOCATION_ALIASES.put("덕천", List.of("북구", "사상구"));
        LOCATION_ALIASES.put("구포", List.of("북구", "사상구"));
        LOCATION_ALIASES.put("만덕", List.of("북구", "금정구"));
        // 사상 일대
        LOCATION_ALIASES.put("사상", List.of("사상구", "북구"));
        LOCATION_ALIASES.put("학장", List.of("사상구", "북구"));
        LOCATION_ALIASES.put("삼락", List.of("사상구", "북구"));
        // 기타
        LOCATION_ALIASES.put("기장", List.of("기장군", "해운대"));
        LOCATION_ALIASES.put("일광", List.of("기장군", "해운대"));
        LOCATION_ALIASES.put("명지", List.of("강서구", "사상구"));
        LOCATION_ALIASES.put("가덕", List.of("강서구"));
        LOCATION_ALIASES.put("시청", List.of("연제구", "동래구", "부산진구"));
    }

    public AiMateService(UpstageApiClient upstageApiClient,
                         ChatMessageRepository chatMessageRepository,
                         PostRepository postRepository,
                         VolunteerRepository volunteerRepository) {
        this.upstageApiClient = upstageApiClient;
        this.chatMessageRepository = chatMessageRepository;
        this.postRepository = postRepository;
        this.volunteerRepository = volunteerRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Upstage Solar LLM 기반 봉사/기부 AI 대화 & 맞춤 파싱 반환
     * - DB에 해당 아이템이 있으면 카드 반환
     * - DB에 없으면 "없음" 명확히 안내 (임의 추천 절대 금지)
     */
    @Transactional
    public AiRecommendResponse getRecommendation(Long userId, AiRecommendRequest request) {
        String userInput = request.getUserInput();

        // 1. 사용자 질문 대화 기록 저장
        chatMessageRepository.save(new ChatMessage(userId, "USER", userInput, null));

        // 2. 봉사/기부 공고 DB 전체 조회
        List<Volunteer> allVolunteers = volunteerRepository.findAll();

        // 3. 키워드 매칭 점수 계산 후 정렬 (> 0점인 것만 선별, 최대 3개)
        List<RecommendedCardDto> candidateCards = new ArrayList<>();

        allVolunteers.stream()
            .filter(v -> calculateMatchScore(v, userInput) > 0)
            .sorted(Comparator.comparingInt(v -> -calculateMatchScore(v, userInput)))
            .limit(3)
            .forEach(v -> candidateCards.add(new RecommendedCardDto(
                v.getId(),
                v.getTitle(),
                v.getLocation() != null ? v.getLocation() : "부산 지역",
                v.getCategory() != null ? v.getCategory() : "VOLUNTEER",
                "LV2_WARMTH"
            )));

        // 4. DB 컨텍스트 빌드 (LLM 시스템 프롬프트에 주입)
        StringBuilder dbContextBuilder = new StringBuilder();
        if (!candidateCards.isEmpty()) {
            dbContextBuilder.append("=== 사용자 요청과 일치하는 DB 봉사/기부 목록 ===\n");
            for (RecommendedCardDto card : candidateCards) {
                dbContextBuilder.append(String.format("- [ID: %d] %s (위치: %s, 카테고리: %s)\n",
                        card.getOpportunityId(), card.getTitle(), card.getRegion(), card.getCategory()));
            }
        } else {
            dbContextBuilder.append("=== 사용자 요청과 일치하는 DB 항목 없음 ===\n");
        }

        // 5. Solar LLM 호출
        AiRecommendResponse aiResponse = upstageApiClient.generateRecommendation(
            userInput, dbContextBuilder.toString(), candidateCards
        );

        // 6. AI 답변 및 추천 카드 JSON DB 저장
        String cardsJson = null;
        try {
            cardsJson = objectMapper.writeValueAsString(aiResponse.getRecommendedCards());
        } catch (Exception ignored) {}

        chatMessageRepository.save(new ChatMessage(userId, "ASSISTANT", aiResponse.getReply(), cardsJson));

        return aiResponse;
    }

    /**
     * 봉사/기부 아이템과 사용자 입력의 연관성 점수 계산
     * - 점수 > 0 이면 매칭, 0 이면 미매칭 (카드 미노출)
     * - 인접 지역 확장: "광안리" → 수영·해운대·남구까지 검색
     */
    private int calculateMatchScore(Volunteer v, String input) {
        if (input == null || input.isBlank()) return 0;
        int score = 0;
        String lowerInput = input.trim().toLowerCase();

        String title = v.getTitle() != null ? v.getTitle().toLowerCase() : "";
        String organizer = v.getOrganizer() != null ? v.getOrganizer().toLowerCase() : "";
        String location = v.getLocation() != null ? v.getLocation().toLowerCase() : "";
        String category = v.getCategory() != null ? v.getCategory().toLowerCase() : "";
        List<String> tags = v.getTags() != null ? v.getTags() : List.of();

        // === 1. 주제어 직통 매칭 (최우선 +60점) ===
        if (lowerInput.contains("유기견") && title.contains("유기견")) score += 60;
        if (lowerInput.contains("플로깅") && title.contains("플로깅")) score += 60;
        if (lowerInput.contains("도시락") && title.contains("도시락")) score += 60;
        if ((lowerInput.contains("학습") || lowerInput.contains("교육") || lowerInput.contains("과외"))
            && (title.contains("학습") || title.contains("교육"))) score += 60;
        if (lowerInput.contains("말벗") && title.contains("말벗")) score += 60;
        if ((lowerInput.contains("생태") || lowerInput.contains("환경"))
            && (title.contains("생태") || title.contains("환경"))) score += 60;
        if (lowerInput.contains("희귀질환") && title.contains("희귀질환")) score += 60;
        if ((lowerInput.contains("결식") || lowerInput.contains("급식")) && title.contains("결식")) score += 60;
        if ((lowerInput.contains("방한") || lowerInput.contains("추위")) && title.contains("방한")) score += 60;
        if (lowerInput.contains("노인") && (title.contains("노인") || title.contains("어르신") || title.contains("말벗"))) score += 60;
        if (lowerInput.contains("어르신") && (title.contains("어르신") || title.contains("말벗"))) score += 60;
        if (lowerInput.contains("아동") && (title.contains("아동") || title.contains("어린이") || title.contains("결식"))) score += 60;
        if (lowerInput.contains("어린이") && (title.contains("아동") || title.contains("어린이"))) score += 60;
        if (lowerInput.contains("유기동물") && title.contains("유기견")) score += 60;
        if (lowerInput.contains("반려") && title.contains("유기견")) score += 50;

        // 기부 카테고리 직통 매칭
        if (lowerInput.contains("유네스코") && (category.contains("unesco") || title.contains("유네스코"))) score += 60;
        if (lowerInput.contains("유산") && (category.contains("legacy") || title.contains("유산"))) score += 60;
        if (lowerInput.contains("문화재") && (category.contains("heritage") || title.contains("문화"))) score += 60;
        if (lowerInput.contains("고향사랑") && (category.contains("hometown") || title.contains("고향"))) score += 60;
        if (lowerInput.contains("기부") && !category.contains("volunteer")) score += 30;
        if (lowerInput.contains("후원") && !category.contains("volunteer")) score += 30;

        // === 2. 지명 별칭 + 인접 지역 확장 매핑 (+40점) ===
        for (Map.Entry<String, List<String>> entry : LOCATION_ALIASES.entrySet()) {
            if (lowerInput.contains(entry.getKey())) {
                for (String mapped : entry.getValue()) {
                    if (location.contains(mapped.toLowerCase())) {
                        score += 40;
                    }
                }
            }
        }

        // === 3. 단어 토큰 기반 동적 매칭 ===
        List<String> stopWords = List.of(
            "봉사", "기부", "후원", "추천", "하고", "싶어", "있어", "알려줘",
            "주말", "모집", "활동", "뭐가", "어떤", "해줘", "있나", "있어요", "근처", "주변"
        );

        String[] tokens = lowerInput.split("\\s+");
        for (String token : tokens) {
            String clean = token.replaceAll("[^가-힣a-zA-Z0-9]", "");
            if (clean.length() >= 2 && !stopWords.contains(clean)) {
                if (title.contains(clean)) score += 25;
                if (location.contains(clean)) score += 20;
                if (organizer.contains(clean)) score += 15;
                for (String t : tags) {
                    if (t.toLowerCase().contains(clean)) score += 15;
                }
            }
        }

        return score;
    }

    private int calculatePostMatchScore(Post p, String input) {
        if (input == null || input.isBlank() || p.getTitle() == null) return 0;
        int score = 0;
        String lowerInput = input.trim().toLowerCase();
        String postTitle = p.getTitle().toLowerCase();
        String postContent = p.getContent() != null ? p.getContent().toLowerCase() : "";

        String[] tokens = lowerInput.split("\\s+");
        for (String token : tokens) {
            String clean = token.replaceAll("[^가-힣a-zA-Z0-9]", "");
            if (clean.length() >= 2) {
                if (postTitle.contains(clean)) score += 20;
                if (postContent.contains(clean)) score += 10;
            }
        }

        return score;
    }

    /**
     * 사용자 대화 히스토리 조회
     */
    public List<ChatMessageResponse> getChatHistory(Long userId) {
        return chatMessageRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtAsc(userId)
                .stream()
                .map(ChatMessageResponse::new)
                .toList();
    }

    /**
     * AI 대화 히스토리 전체 삭제 (초기화)
     */
    @Transactional
    public void clearChatHistory(Long userId) {
        chatMessageRepository.deleteAll();
    }
}
