package com.pixelcare.domain.management.controller;

import com.pixelcare.domain.management.dto.CenterDashboardResponse;
import com.pixelcare.domain.management.dto.OrganizationResponse;
import com.pixelcare.domain.management.dto.OrganizationUpdateRequest;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrganizationController {

    private final AuthGuard authGuard;
    private final ManagementService service;

    public OrganizationController(AuthGuard authGuard, ManagementService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @GetMapping("/organizations/{organizationId}")
    public ApiResponse<OrganizationResponse> publicDetail(@PathVariable Long organizationId) {
        return ApiResponse.success(service.publicOrganization(organizationId));
    }

    @GetMapping("/manager/organizations")
    public ApiResponse<List<OrganizationResponse>> managed(HttpServletRequest request) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.managedOrganizations(user.id()));
    }

    @PatchMapping("/manager/organizations/{organizationId}")
    public ApiResponse<OrganizationResponse> update(
            HttpServletRequest request,
            @PathVariable Long organizationId,
            @RequestBody OrganizationUpdateRequest body
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.updateOrganization(user.id(), organizationId, body));
    }

    @GetMapping("/manager/organizations/{organizationId}/dashboard")
    public ApiResponse<CenterDashboardResponse> dashboard(
            HttpServletRequest request,
            @PathVariable Long organizationId
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.dashboard(user.id(), organizationId));
    }
}
