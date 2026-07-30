package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.service.PostService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operator/community/posts")
public class OperatorPostController {

    private final AuthGuard authGuard;
    private final PostService service;

    public OperatorPostController(AuthGuard authGuard, PostService service) {
        this.authGuard = authGuard;
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<PostResponse>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.getPostsForOperator(page, size));
    }

    @PatchMapping("/{id}")
    public ApiResponse<PostResponse> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody PostCreateRequest body
    ) {
        CurrentUser operator = authGuard.requireRole(request, "OPERATOR");
        return ApiResponse.success(service.updatePost(operator.id(), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        CurrentUser operator = authGuard.requireRole(request, "OPERATOR");
        service.deletePostAsUser(operator, id);
        return ApiResponse.success("커뮤니티 글이 삭제되었습니다.");
    }
}
