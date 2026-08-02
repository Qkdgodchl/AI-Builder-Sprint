package com.pixelcare.domain.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PledgeIntent(
        String pledgeType,
        String beneficiary,
        BigDecimal amount,
        String frequency,
        LocalDate startDate,
        String region,
        String rewardPreference,
        Boolean taxDeductionConsent,
        Boolean privacyConsent,
        String specialConditions,
        String giftItem,
        String localGovCode,
        BigDecimal taxCreditAmount,
        String heritageTarget,
        String bequestType,
        List<String> missingFields
) {
    public PledgeIntent {
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }
}
