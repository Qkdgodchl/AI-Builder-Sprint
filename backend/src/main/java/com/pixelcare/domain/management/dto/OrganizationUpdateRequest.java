package com.pixelcare.domain.management.dto;

public record OrganizationUpdateRequest(
        String name,
        String phone,
        String email,
        String address,
        String description,
        String homepageUrl
) {}
