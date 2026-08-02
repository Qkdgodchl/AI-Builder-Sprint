package com.pixelcare.domain.ai.controller;

import com.pixelcare.domain.ai.dto.ConsultationRequest;
import com.pixelcare.domain.ai.dto.ConsultationResponse;
import com.pixelcare.domain.ai.dto.PledgeIntent;
import com.pixelcare.domain.ai.service.AiConsultationService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/consultations")
public class AiConsultationController {

    private final AuthGuard authGuard;
    private final AiConsultationService service;

    public AiConsultationController(AuthGuard authGuard, AiConsultationService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultationResponse> start(HttpServletRequest request,
                                                   @Valid @RequestBody ConsultationRequest body) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.start(user.id(), body.message(), body.externalAiConsent()));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<ConsultationResponse> message(HttpServletRequest request, @PathVariable Long id,
                                                     @Valid @RequestBody ConsultationRequest body) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.addMessage(id, user.id(), body.message(), body.externalAiConsent()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConsultationResponse> get(HttpServletRequest request, @PathVariable Long id) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.get(id, user.id()));
    }

    @PatchMapping("/{id}/intent")
    public ApiResponse<ConsultationResponse> update(HttpServletRequest request, @PathVariable Long id,
                                                    @RequestBody PledgeIntent body) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.update(id, user.id(), body));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<ConsultationResponse> confirm(HttpServletRequest request, @PathVariable Long id) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(service.confirm(id, user.id()));
    }
}
