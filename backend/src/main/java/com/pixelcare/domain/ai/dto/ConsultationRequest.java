package com.pixelcare.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultationRequest(
        @NotBlank(message = "정리할 기부·봉사 의사를 입력해주세요.") String message,
        boolean externalAiConsent
) {}
