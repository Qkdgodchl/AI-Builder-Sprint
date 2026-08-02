package com.pixelcare.domain.application.controller;

import com.pixelcare.domain.application.dto.ApplicationDecisionRequest;
import com.pixelcare.domain.application.dto.ApplicationResponse;
import com.pixelcare.domain.application.service.ApplicationService;
import com.pixelcare.domain.clm.dto.ClmDocumentResponseDto;
import com.pixelcare.domain.clm.service.ClmDocumentService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager")
public class ManagerApplicationReviewController {

    private final AuthGuard authGuard;
    private final ApplicationService service;
    private final ClmDocumentService clmDocumentService;

    public ManagerApplicationReviewController(
            AuthGuard authGuard,
            ApplicationService service,
            ClmDocumentService clmDocumentService
    ) {
        this.authGuard = authGuard;
        this.service = service;
        this.clmDocumentService = clmDocumentService;
    }

    @GetMapping("/applications/{publicId}/clm-documents")
    public ApiResponse<List<ClmDocumentResponseDto>> clmDocuments(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser manager = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(
                clmDocumentService.getManagerApplicationDocuments(publicId, manager)
        );
    }

    @GetMapping("/opportunities/{opportunityId}/applications")
    public ApiResponse<List<ApplicationResponse>> list(
            HttpServletRequest request,
            @PathVariable Long opportunityId
    ) {
        CurrentUser manager = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.managerApplications(manager.id(), opportunityId));
    }

    @GetMapping("/applications/{publicId}")
    public ApiResponse<ApplicationResponse> detail(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser manager = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.detail(manager.id(), true, publicId));
    }

    @PostMapping("/applications/{publicId}/approve")
    public ApiResponse<ApplicationResponse> approve(
            HttpServletRequest request,
            @PathVariable String publicId,
            @RequestBody(required = false) ApplicationDecisionRequest body
    ) {
        CurrentUser manager = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.decide(
                manager.id(),
                publicId,
                "APPROVED",
                body == null ? null : body.reason()
        ));
    }

    @PostMapping("/applications/{publicId}/request-revision")
    public ApiResponse<ApplicationResponse> requestRevision(
            HttpServletRequest request,
            @PathVariable String publicId,
            @RequestBody ApplicationDecisionRequest body
    ) {
        CurrentUser manager = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.decide(
                manager.id(),
                publicId,
                "REVISION_REQUESTED",
                body.reason()
        ));
    }

    @PostMapping("/applications/{publicId}/reject")
    public ApiResponse<ApplicationResponse> reject(
            HttpServletRequest request,
            @PathVariable String publicId,
            @RequestBody ApplicationDecisionRequest body
    ) {
        CurrentUser manager = authGuard.requireRole(request, "CENTER_MANAGER");
        return ApiResponse.success(service.decide(
                manager.id(),
                publicId,
                "REJECTED",
                body.reason()
        ));
    }
}
