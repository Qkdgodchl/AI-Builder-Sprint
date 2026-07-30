package com.pixelcare.domain.opportunity.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OpportunityResponse(
        Long id,
        Long organizationId,
        String organizationName,
        String type,
        String category,
        String title,
        String summary,
        String description,
        String region,
        String location,
        String participationMode,
        Integer recruitmentCapacity,
        LocalDateTime recruitmentStartDateTime,
        LocalDateTime recruitmentEndDateTime,
        LocalDateTime activityStartDateTime,
        LocalDateTime activityEndDateTime,
        String eligibility,
        Long targetAmount,
        Long currentAmount,
        String cancellationPolicy,
        String status,
        long applicantCount,
        List<RequiredDocumentResponse> requiredDocuments,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
    public record RequiredDocumentResponse(
            String code,
            String name,
            String description,
            boolean required,
            int displayOrder
    ) {}
}
