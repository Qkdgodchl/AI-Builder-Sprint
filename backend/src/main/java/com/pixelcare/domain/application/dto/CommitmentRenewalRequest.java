package com.pixelcare.domain.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 정기 약정 갱신 요청.
 * 금액과 주기를 비워 두면 같은 조건으로 기간만 연장하고,
 * 값을 채워 보내면 조건이 바뀐 갱신이라 다시 서명을 받는다.
 */
public record CommitmentRenewalRequest(
        LocalDate effectiveTo,
        BigDecimal pledgeAmount,
        String pledgeFrequency
) {}
