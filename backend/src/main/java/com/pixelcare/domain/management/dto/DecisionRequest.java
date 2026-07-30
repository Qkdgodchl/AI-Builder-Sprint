package com.pixelcare.domain.management.dto;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(
        @NotBlank(message = "처리 사유는 필수입니다.")
        String reason
) {}
