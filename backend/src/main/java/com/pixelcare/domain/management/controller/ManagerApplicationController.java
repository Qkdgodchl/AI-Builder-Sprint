package com.pixelcare.domain.management.controller;

import com.pixelcare.domain.management.dto.ManagerApplicationRequest;
import com.pixelcare.domain.management.dto.ManagerApplicationResponse;
import com.pixelcare.domain.management.service.ManagementService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager-applications")
public class ManagerApplicationController {

    private final AuthGuard authGuard;
    private final ManagementService service;

    public ManagerApplicationController(AuthGuard authGuard, ManagementService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ManagerApplicationResponse> apply(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ManagerApplicationRequest request
    ) {
        CurrentUser user = authGuard.requireUser(httpRequest);
        return ApiResponse.success(service.applyManager(user.id(), request));
    }

    @GetMapping("/me")
    public ApiResponse<List<ManagerApplicationResponse>> me(HttpServletRequest request) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.myManagerApplications(user.id()));
    }

    @GetMapping("/{publicId}")
    public ApiResponse<ManagerApplicationResponse> detail(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.getManagerApplication(
                publicId,
                user.id(),
                user.hasRole("OPERATOR")
        ));
    }

    @PostMapping("/{publicId}/cancel")
    public ApiResponse<ManagerApplicationResponse> cancel(
            HttpServletRequest request,
            @PathVariable String publicId
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.cancelManagerApplication(publicId, user.id()));
    }
}
