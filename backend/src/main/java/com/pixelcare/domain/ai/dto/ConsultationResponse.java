package com.pixelcare.domain.ai.dto;

public record ConsultationResponse(
        Long id,
        String status,
        String summary,
        PledgeIntent intent,
        String assistantMessage,
        String source
) {}
