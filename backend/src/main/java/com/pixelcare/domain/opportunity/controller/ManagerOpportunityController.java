package com.pixelcare.domain.opportunity.controller;

import com.pixelcare.domain.opportunity.dto.OpportunityRequest;
import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager")
public class ManagerOpportunityController {

    private final AuthGuard authGuard;
    private final OpportunityService service;

    public ManagerOpportunityController(AuthGuard authGuard, OpportunityService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @PostMapping("/organizations/{organizationId}/opportunities")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpportunityResponse> create(
            HttpServletRequest httpRequest,
            @PathVariable Long organizationId,
            @Valid @RequestBody OpportunityRequest request
    ) {
        CurrentUser user = authGuard.requireRole(httpRequest, "CENTER_MANAGER");
        return ApiResponse.success(service.create(user.id(), organizationId, request));
    }

    @GetMapping("/organizations/{organizationId}/opportunities")
    public ApiResponse<List<OpportunityResponse>> list(
            HttpServletRequest request,
            @PathVariable Long organizationId
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.managerList(user.id(), organizationId));
    }

    @GetMapping("/opportunities/{id}")
    public ApiResponse<OpportunityResponse> detail(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.managerDetail(user.id(), id));
    }

    @PatchMapping("/opportunities/{id}")
    public ApiResponse<OpportunityResponse> update(
            HttpServletRequest httpRequest,
            @PathVariable Long id,
            @Valid @RequestBody OpportunityRequest request
    ) {
        CurrentUser user = authGuard.requireRole(httpRequest, "CENTER_MANAGER");
        return ApiResponse.success(service.update(user.id(), id, request));
    }

    @PostMapping("/opportunities/{id}/publish")
    public ApiResponse<OpportunityResponse> publish(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.publish(user.id(), id));
    }

    @PostMapping("/opportunities/{id}/close")
    public ApiResponse<OpportunityResponse> close(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.close(user.id(), id));
    }

    @PostMapping("/opportunities/{id}/cancel")
    public ApiResponse<OpportunityResponse> cancel(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        CurrentUser user = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.cancel(user.id(), id));
    }
}
