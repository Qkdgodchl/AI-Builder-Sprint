package com.pixelcare.domain.opportunity.controller;

import com.pixelcare.domain.opportunity.dto.OpportunityRequest;
import com.pixelcare.domain.opportunity.dto.OpportunityResponse;
import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/opportunities")
public class OperatorOpportunityController {

    private final AuthGuard authGuard;
    private final OpportunityService service;

    public OperatorOpportunityController(AuthGuard authGuard, OpportunityService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<OpportunityResponse>> list(HttpServletRequest request) {
        authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.operatorList());
    }

    @GetMapping("/{id}")
    public ApiResponse<OpportunityResponse> detail(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.operatorDetail(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<OpportunityResponse> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody OpportunityRequest body
    ) {
        CurrentUser operator = authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.operatorUpdate(operator.id(), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        CurrentUser operator = authGuard.requireRole(request, "OPERATOR");
        service.operatorDelete(operator.id(), id);
        return ApiResponse.success("모집글이 삭제되었습니다.");
    }
}
