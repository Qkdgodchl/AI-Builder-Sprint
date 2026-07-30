package com.pixelcare.domain.application.dto;

import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record ApplicationCreateRequest(
        Long consultationId,
        LocalDate participationDate,
        String specialConditions,
        @AssertTrue(message = "개인정보 수집 동의가 필요합니다.")
        boolean privacyConsent,
        @AssertTrue(message = "제3자 제공 동의가 필요합니다.")
        boolean thirdPartyConsent,
        boolean portraitConsent
) {}
