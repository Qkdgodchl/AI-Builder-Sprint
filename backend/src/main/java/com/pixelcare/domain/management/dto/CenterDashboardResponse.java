package com.pixelcare.domain.management.dto;

import java.math.BigDecimal;

public record CenterDashboardResponse(
        Long organizationId,
        long publicOpportunities,
        long closedOpportunities,
        long pendingApplications,
        long monthlyParticipants,
        long totalCommitments,
        long signedCommitments,
        long awaitingSignature,
        BigDecimal signedPledgeAmount,
        long renewalDueSoon
) {}
