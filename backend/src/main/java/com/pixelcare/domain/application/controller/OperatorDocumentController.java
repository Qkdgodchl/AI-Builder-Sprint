package com.pixelcare.domain.application.controller;

import com.pixelcare.domain.application.dto.ApplicationResponse;
import com.pixelcare.domain.application.service.ApplicationService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/documents")
public class OperatorDocumentController {

    private final AuthGuard authGuard;
    private final ApplicationService service;

    public OperatorDocumentController(AuthGuard authGuard, ApplicationService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ApplicationResponse>> list(HttpServletRequest request) {
        authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.operatorApplications());
    }

    @GetMapping("/{applicationPublicId}")
    public ApiResponse<ApplicationResponse> detail(
            HttpServletRequest request,
            @PathVariable String applicationPublicId
    ) {
        authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.operatorDetail(applicationPublicId));
    }
}
