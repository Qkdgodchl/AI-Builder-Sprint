package com.pixelcare.domain.application.dto;

import java.time.LocalDate;

public record CommitmentUpdateRequest(
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String specialConditions,
        Boolean privacyConsent,
        Boolean thirdPartyConsent,
        Boolean portraitConsent
) {}
