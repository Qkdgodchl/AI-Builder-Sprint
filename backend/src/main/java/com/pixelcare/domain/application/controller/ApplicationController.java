package com.pixelcare.domain.application.controller;

import com.pixelcare.domain.application.dto.*;
import com.pixelcare.domain.application.service.ApplicationService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApplicationController {

    private final AuthGuard authGuard;
    private final ApplicationService service;

    public ApplicationController(AuthGuard authGuard, ApplicationService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @PostMapping("/opportunities/{opportunityId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationResponse> apply(
            HttpServletRequest httpRequest,
            @PathVariable Long opportunityId,
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        CurrentUser user = authGuard.requireUser(httpRequest);
        return ApiResponse.success(service.apply(user.id(), opportunityId, request));
    }

    @GetMapping("/applications/me")
    public ApiResponse<List<ApplicationResponse>> me(HttpServletRequest request) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.myApplications(user.id()));
    }

    @GetMapping("/applications/{publicId}")
    public ApiResponse<ApplicationResponse> detail(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.detail(
                user.id(),
                user.hasRole("CENTER_MANAGER"),
                publicId
        ));
    }

    @PostMapping("/applications/{publicId}/cancel")
    public ApiResponse<ApplicationResponse> cancel(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.cancel(user.id(), publicId));
    }

    @GetMapping("/commitments/{publicId}")
    public ApiResponse<ApplicationResponse.CommitmentSummary> commitment(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.commitment(user.id(), publicId));
    }

    @PatchMapping("/commitments/{publicId}")
    public ApiResponse<ApplicationResponse.CommitmentSummary> updateCommitment(
            HttpServletRequest request,
            @PathVariable String publicId,
            @RequestBody CommitmentUpdateRequest body
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.updateCommitment(user.id(), publicId, body));
    }

    @PostMapping("/commitments/{publicId}/submit-review")
    public ApiResponse<ApplicationResponse.CommitmentSummary> submitCommitment(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.submitCommitment(user.id(), publicId));
    }

    @PostMapping("/commitments/{publicId}/renew")
    public ApiResponse<ApplicationResponse.CommitmentSummary> renewCommitment(
            HttpServletRequest request,
            @PathVariable String publicId,
            @RequestBody(required = false) CommitmentRenewalRequest body
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.renewCommitment(user.id(), publicId, body));
    }
}
