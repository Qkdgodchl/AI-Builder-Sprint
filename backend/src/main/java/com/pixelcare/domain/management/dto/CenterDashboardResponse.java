package com.pixelcare.domain.management.dto;

public record CenterDashboardResponse(
        Long organizationId,
        long publicOpportunities,
        long closedOpportunities,
        long pendingApplications,
        long monthlyParticipants
) {}
