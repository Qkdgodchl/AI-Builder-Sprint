package com.pixelcare.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AiRecommendRequest {

    @NotBlank(message = "질문 문장을 입력해 주세요.")
    private String userInput;

    public AiRecommendRequest() {}

    public AiRecommendRequest(String userInput) {
        this.userInput = userInput;
    }

    public String getUserInput() { return userInput; }
}
