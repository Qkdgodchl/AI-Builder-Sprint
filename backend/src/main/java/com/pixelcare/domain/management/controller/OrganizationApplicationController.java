package com.pixelcare.domain.management.controller;

import com.pixelcare.domain.management.dto.OrganizationApplicationRequest;
import com.pixelcare.domain.management.dto.OrganizationApplicationResponse;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organization-applications")
public class OrganizationApplicationController {

    private final AuthGuard authGuard;
    private final ManagementService service;

    public OrganizationApplicationController(AuthGuard authGuard, ManagementService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrganizationApplicationResponse> apply(
            HttpServletRequest request,
            @Valid @RequestBody OrganizationApplicationRequest body
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.applyOrganization(user.id(), body));
    }

    @GetMapping("/me")
    public ApiResponse<List<OrganizationApplicationResponse>> mine(HttpServletRequest request) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.myOrganizationApplications(user.id()));
    }
}
