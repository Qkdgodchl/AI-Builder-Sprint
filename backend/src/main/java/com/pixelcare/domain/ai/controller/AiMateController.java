package com.pixelcare.domain.ai.controller;

import com.pixelcare.domain.ai.dto.AiRecommendRequest;
import com.pixelcare.domain.ai.dto.AiRecommendResponse;
import com.pixelcare.domain.ai.dto.ChatMessageResponse;
import com.pixelcare.domain.ai.service.AiMateService;
import com.pixelcare.global.common.ApiResponse;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiMateController {

    private final AiMateService aiMateService;
    private final AuthGuard authGuard;

    public AiMateController(AiMateService aiMateService, AuthGuard authGuard) {
        this.aiMateService = aiMateService;
        this.authGuard = authGuard;
    }

    /**
     * Upstage Solar LLM 기반 봉사/기부 맞춤 큐레이션 대화
     * POST /api/ai/recommend
     */
    @PostMapping("/recommend")
    public ApiResponse<AiRecommendResponse> recommend(HttpServletRequest httpRequest,
                                                      @Valid @RequestBody AiRecommendRequest request) {
        CurrentUser user = authGuard.requireUser(httpRequest);
        AiRecommendResponse response = aiMateService.getRecommendation(user.id(), request);
        return ApiResponse.success(response);
    }

    /**
     * AI 마스코트 챗봇 대화 히스토리 조회
     * GET /api/ai/messages
     */
    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageResponse>> getChatHistory(HttpServletRequest request) {
        CurrentUser user = authGuard.requireUser(request);
        List<ChatMessageResponse> history = aiMateService.getChatHistory(user.id());
        return ApiResponse.success(history);
    }

    /**
     * AI 마스코트 챗봇 대화 히스토리 전체 삭제 (초기화)
     * DELETE /api/ai/messages
     */
    @DeleteMapping("/messages")
    public ApiResponse<String> clearChatHistory(HttpServletRequest request) {
        CurrentUser user = authGuard.requireUser(request);
        aiMateService.clearChatHistory(user.id());
        return ApiResponse.success("AI 대화 내역이 성공적으로 초기화되었습니다.");
    }
}
