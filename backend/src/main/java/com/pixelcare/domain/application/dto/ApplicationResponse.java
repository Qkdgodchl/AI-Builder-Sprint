package com.pixelcare.domain.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationResponse(
        String publicId,
        Long opportunityId,
        String opportunityTitle,
        String opportunityType,
        Long organizationId,
        String organizationName,
        Long applicantUserId,
        String applicantName,
        String applicantEmail,
        LocalDate participationDate,
        String specialConditions,
        boolean privacyConsent,
        boolean thirdPartyConsent,
        boolean portraitConsent,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewReason,
        CommitmentSummary commitment,
        List<DocumentSummary> documents,
        LocalDateTime createdAt
) {
    public record CommitmentSummary(
            String publicId,
            String status,
            String title,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            int version,
            String renderedContent
    ) {}

    public record DocumentSummary(
            String code,
            String name,
            String description,
            boolean required,
            String status
    ) {}
}
