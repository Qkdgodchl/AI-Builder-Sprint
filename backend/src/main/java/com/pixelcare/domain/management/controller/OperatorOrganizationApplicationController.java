package com.pixelcare.domain.management.controller;

import com.pixelcare.domain.management.dto.DecisionRequest;
import com.pixelcare.domain.management.dto.OrganizationApplicationResponse;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/organization-applications")
public class OperatorOrganizationApplicationController {

    private final AuthGuard authGuard;
    private final ManagementService service;

    public OperatorOrganizationApplicationController(AuthGuard authGuard, ManagementService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<OrganizationApplicationResponse>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status
    ) {
        authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.organizationApplicationsForOperator(status));
    }

    @PostMapping("/{publicId}/approve")
    public ApiResponse<OrganizationApplicationResponse> approve(
            HttpServletRequest request,
            @PathVariable String publicId,
            @Valid @RequestBody DecisionRequest decision
    ) {
        CurrentUser operator = authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.decideOrganizationApplication(
                publicId, operator.id(), "APPROVED", decision.reason()
        ));
    }

    @PostMapping("/{publicId}/reject")
    public ApiResponse<OrganizationApplicationResponse> reject(
            HttpServletRequest request,
            @PathVariable String publicId,
            @Valid @RequestBody DecisionRequest decision
    ) {
        CurrentUser operator = authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.decideOrganizationApplication(
                publicId, operator.id(), "REJECTED", decision.reason()
        ));
    }
}
