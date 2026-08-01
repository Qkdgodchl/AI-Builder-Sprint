package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.CommentCreateRequest;
import com.pixelcare.domain.community.dto.CommentResponse;
import com.pixelcare.domain.community.service.CommentService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;
    private final AuthGuard authGuard;

    public CommentController(CommentService commentService, AuthGuard authGuard) {
        this.commentService = commentService;
        this.authGuard = authGuard;
    }

    @GetMapping({"/api/posts/{postId}/comments", "/api/v1/posts/{postId}/comments"})
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getComments(postId);
        return ApiResponse.success(comments);
    }

    @PostMapping({"/api/posts/{postId}/comments", "/api/v1/posts/{postId}/comments"})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.resolveUser(httpRequest);
        String nickname = currentUser != null ? currentUser.nickname() : "익명 용사";
        String badge = "LV2_WARMTH"; // 기본 인증 뱃지
        CommentCreateRequest sanitizedRequest = currentUser == null
                ? request
                : new CommentCreateRequest(request.getContent(), currentUser.nickname(), badge);

        CommentResponse comment = commentService.createComment(postId, sanitizedRequest, nickname, badge);
        return ApiResponse.success(comment, "댓글이 등록되었습니다.");
    }

    @DeleteMapping({"/api/comments/{commentId}", "/api/v1/comments/{commentId}"})
    public ApiResponse<Void> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest httpRequest
    ) {
        CurrentUser currentUser = authGuard.resolveUser(httpRequest);
        String deletedBy = currentUser != null ? currentUser.nickname() : "GUEST";

        commentService.deleteComment(commentId, deletedBy);
        return ApiResponse.success("댓글이 삭제되었습니다.");
    }
}
