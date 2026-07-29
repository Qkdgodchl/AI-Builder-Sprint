package com.pixelcare.domain.ai;

import com.pixelcare.domain.ai.dto.AiRecommendRequest;
import com.pixelcare.domain.ai.dto.AiRecommendResponse;
import com.pixelcare.domain.ai.dto.ChatMessageResponse;
import com.pixelcare.domain.ai.service.AiMateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AiMateServiceTest {

    @Autowired
    private AiMateService aiMateService;

    @Test
    @DisplayName("Upstage Solar LLM 챗봇 추천 및 대화 이력 저장 테스트")
    void getRecommendation_success() {
        // given
        AiRecommendRequest request = new AiRecommendRequest("부산 금정구근처 유기견 봉사활동 추천해줘");

        // when
        AiRecommendResponse response = aiMateService.getRecommendation(100L, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReply()).isNotBlank();
        assertThat(response.getRecommendedCards()).isNotEmpty();

        // 대화 히스토리 DB 저장 검증
        List<ChatMessageResponse> history = aiMateService.getChatHistory(100L);
        assertThat(history).hasSize(2); // USER 질문 + ASSISTANT 답변
        assertThat(history.get(0).getSender()).isEqualTo("USER");
        assertThat(history.get(1).getSender()).isEqualTo("ASSISTANT");
    }
}
