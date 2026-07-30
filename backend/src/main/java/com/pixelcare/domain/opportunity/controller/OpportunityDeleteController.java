package com.pixelcare.domain.opportunity.controller;

import com.pixelcare.domain.opportunity.service.OpportunityService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityDeleteController {

    private final AuthGuard authGuard;
    private final OpportunityService service;

    public OpportunityDeleteController(AuthGuard authGuard, OpportunityService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        CurrentUser user = authGuard.requireUser(request);
        service.deleteAsUser(user, id);
        return ApiResponse.success("모집글이 삭제되었습니다.");
    }
}
