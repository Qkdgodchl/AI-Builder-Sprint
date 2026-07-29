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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiMateService {

    private final UpstageApiClient upstageApiClient;
    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;

    public AiMateService(UpstageApiClient upstageApiClient,
                         ChatMessageRepository chatMessageRepository,
                         PostRepository postRepository) {
        this.upstageApiClient = upstageApiClient;
        this.chatMessageRepository = chatMessageRepository;
        this.postRepository = postRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Upstage Solar LLM 기반 봉사/기부 AI 대화 & 맞춤 파싱 반환
     */
    @Transactional
    public AiRecommendResponse getRecommendation(Long userId, AiRecommendRequest request) {
        String userInput = request.getUserInput();

        // 1. 사용자 질문 대화 기록 저장
        chatMessageRepository.save(new ChatMessage(userId, "USER", userInput, null));

        // 2. 내부 DB 데이터 조회 (상위 활성 모집글/게시글)
        List<Post> dbPosts = postRepository.findByIsDeletedFalse(PageRequest.of(0, 5)).getContent();

        StringBuilder dbContextBuilder = new StringBuilder();
        List<RecommendedCardDto> candidateCards = new ArrayList<>();

        for (Post post : dbPosts) {
            dbContextBuilder.append(String.format("- [ID: %d] %s (카테고리: %s, 작성자: %s, 뱃지: %s)\n",
                    post.getId(), post.getTitle(), post.getCategory().name(), post.getAuthorNickname(), post.getAuthorBadge()));

            candidateCards.add(new RecommendedCardDto(
                    post.getId(),
                    post.getTitle(),
                    "부산 지역",
                    post.getCategory().name(),
                    post.getAuthorBadge()
            ));
        }

        if (candidateCards.isEmpty()) {
            // 더미 픽셀 데이터 기본제공
            candidateCards.add(new RecommendedCardDto(1L, "🐕 부산 북구 유기견 보육원 주말 봉사", "부산 북구", "VOLUNTEER", "LV1_SEED"));
            candidateCards.add(new RecommendedCardDto(2L, "🍲 금정구 독거어르신 온기 도시락 배달", "부산 금정구", "VOLUNTEER", "LV2_WARMTH"));
        }

        // 3. Solar LLM 호출 (System Prompt 내 DB Context 주입)
        AiRecommendResponse aiResponse = upstageApiClient.generateRecommendation(userInput, dbContextBuilder.toString(), candidateCards);

        // 4. AI 답변 및 추천 카드 JSON DB 저장
        String cardsJson = null;
        try {
            cardsJson = objectMapper.writeValueAsString(aiResponse.getRecommendedCards());
        } catch (Exception ignored) {}

        chatMessageRepository.save(new ChatMessage(userId, "ASSISTANT", aiResponse.getReply(), cardsJson));

        return aiResponse;
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
}
