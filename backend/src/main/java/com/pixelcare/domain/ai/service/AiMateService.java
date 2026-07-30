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

    // 지명 별칭 매핑: "부산대" → "금정구", "해운대" → "해운대" 등
    private static final Map<String, List<String>> LOCATION_ALIASES = Map.of(
        "부산대", List.of("금정구", "장전동", "사상구"),
        "서면", List.of("부산진구", "부전동"),
        "남포", List.of("중구", "영도구"),
        "해운대", List.of("해운대"),
        "사직", List.of("동래구"),
        "연산", List.of("연제구"),
        "노포", List.of("금정구"),
        "덕천", List.of("북구"),
        "동래", List.of("동래구")
    );

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
        List<Post> dbPosts = postRepository.findByIsDeletedFalse(PageRequest.of(0, 10)).getContent();

        // 3. 키워드 매칭 점수 계산 후 정렬 (> 0점인 것만 선별)
        List<RecommendedCardDto> candidateCards = new ArrayList<>();

        allVolunteers.stream()
            .filter(v -> calculateMatchScore(v, userInput) > 0)
            .sorted(Comparator.comparingInt(v -> -calculateMatchScore(v, userInput)))
            .limit(3)  // 최대 3개 카드만 노출
            .forEach(v -> candidateCards.add(new RecommendedCardDto(
                v.getId(),
                v.getTitle(),
                v.getLocation() != null ? v.getLocation() : "부산 지역",
                v.getCategory() != null ? v.getCategory() : "VOLUNTEER",
                "LV2_WARMTH"
            )));

        // 커뮤니티 게시글은 AI 추천 카드 대상에서 제외 (volunteers DB만 사용)


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

        // === 1. 구체적 주제어 직통 매칭 (최우선 +60점) ===
        if (lowerInput.contains("유기견") && title.contains("유기견")) score += 60;
        if (lowerInput.contains("플로깅") && title.contains("플로깅")) score += 60;
        if (lowerInput.contains("도시락") && title.contains("도시락")) score += 60;
        if ((lowerInput.contains("학습") || lowerInput.contains("교육") || lowerInput.contains("과외")) 
            && (title.contains("학습") || title.contains("교육"))) score += 60;
        if (lowerInput.contains("말벗") && title.contains("말벗")) score += 60;
        if ((lowerInput.contains("생태") || lowerInput.contains("환경")) 
            && (title.contains("생태") || title.contains("환경"))) score += 60;
        if (lowerInput.contains("희귀질환") && title.contains("희귀질환")) score += 60;
        if ((lowerInput.contains("결식") || lowerInput.contains("급식")) 
            && title.contains("결식")) score += 60;
        if ((lowerInput.contains("방한") || lowerInput.contains("추위")) 
            && title.contains("방한")) score += 60;
        if (lowerInput.contains("노인") && (title.contains("노인") || title.contains("어르신") || title.contains("말벗"))) score += 60;
        if (lowerInput.contains("어르신") && (title.contains("어르신") || title.contains("말벗"))) score += 60;
        if (lowerInput.contains("아동") && (title.contains("아동") || title.contains("어린이") || title.contains("결식"))) score += 60;
        if (lowerInput.contains("어린이") && (title.contains("아동") || title.contains("어린이"))) score += 60;

        // 기부 카테고리 직통 매칭
        if (lowerInput.contains("유네스코") && (category.contains("unesco") || title.contains("유네스코"))) score += 60;
        if (lowerInput.contains("유산") && (category.contains("legacy") || title.contains("유산"))) score += 60;
        if (lowerInput.contains("문화재") && (category.contains("heritage") || title.contains("문화"))) score += 60;
        if (lowerInput.contains("고향사랑") && (category.contains("hometown") || title.contains("고향"))) score += 60;
        if (lowerInput.contains("기부") && !category.contains("volunteer")) score += 30;
        if (lowerInput.contains("후원") && !category.contains("volunteer")) score += 30;

        // === 2. 지명 별칭 매핑 매칭 (+40점) ===
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
            "주말", "모집", "활동", "뭐가", "어떤", "해줘", "있나", "있어요"
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
