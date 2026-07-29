package com.pixelcare.domain.ai.controller;

import com.pixelcare.domain.ai.dto.AiRecommendRequest;
import com.pixelcare.domain.ai.dto.AiRecommendResponse;
import com.pixelcare.domain.ai.dto.ChatMessageResponse;
import com.pixelcare.domain.ai.service.AiMateService;
import com.pixelcare.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiMateController {

    private final AiMateService aiMateService;

    public AiMateController(AiMateService aiMateService) {
        this.aiMateService = aiMateService;
    }

    /**
     * Upstage Solar LLM 기반 봉사/기부 맞춤 큐레이션 대화
     * POST /api/ai/recommend
     */
    @PostMapping("/recommend")
    public ApiResponse<AiRecommendResponse> recommend(@Valid @RequestBody AiRecommendRequest request) {
        // TODO: 로그인 인증 적용 후 유저 ID 연동
        Long dummyUserId = 1L;
        AiRecommendResponse response = aiMateService.getRecommendation(dummyUserId, request);
        return ApiResponse.success(response);
    }

    /**
     * AI 마스코트 챗봇 대화 히스토리 조회
     * GET /api/ai/messages
     */
    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageResponse>> getChatHistory() {
        Long dummyUserId = 1L;
        List<ChatMessageResponse> history = aiMateService.getChatHistory(dummyUserId);
        return ApiResponse.success(history);
    }
}
