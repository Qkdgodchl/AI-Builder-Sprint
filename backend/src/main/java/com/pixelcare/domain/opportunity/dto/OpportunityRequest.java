package com.pixelcare.domain.opportunity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public record OpportunityRequest(
        @NotBlank(message = "모집글 유형은 필수입니다.")
        String type,
        String category,
        @NotBlank(message = "제목은 필수입니다.")
        String title,
        String summary,
        @NotBlank(message = "상세 설명은 필수입니다.")
        String description,
        String region,
        String location,
        String participationMode,
        @Min(value = 1, message = "모집 정원은 1명 이상이어야 합니다.")
        Integer recruitmentCapacity,
        LocalDateTime recruitmentStartDateTime,
        LocalDateTime recruitmentEndDateTime,
        LocalDateTime activityStartDateTime,
        LocalDateTime activityEndDateTime,
        String eligibility,
        Long targetAmount,
        String cancellationPolicy,
        List<RequiredDocumentRequest> requiredDocuments
) {
    public record RequiredDocumentRequest(
            @NotBlank(message = "문서 코드는 필수입니다.")
            String code,
            @NotBlank(message = "문서명은 필수입니다.")
            String name,
            String description,
            Boolean required
    ) {}
}
