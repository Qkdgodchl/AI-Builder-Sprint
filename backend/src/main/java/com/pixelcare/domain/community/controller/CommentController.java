package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.CommentCreateRequest;
import com.pixelcare.domain.community.dto.CommentResponse;
import com.pixelcare.domain.community.service.CommentService;
import com.pixelcare.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 게시글 댓글 목록 조회
     * GET /api/posts/{postId}/comments
     */
    @GetMapping("/api/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getComments(postId);
        return ApiResponse.success(comments);
    }

    /**
     * 댓글 작성
     * POST /api/posts/{postId}/comments
     */
    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        // TODO: 로그인 인증 연동 후 유저 닉네임 및 뱃지 정보 세팅
        String dummyNickname = "행복봉사자";
        String dummyBadge = "LV1_SEED";
        CommentResponse comment = commentService.createComment(postId, request, dummyNickname, dummyBadge);
        return ApiResponse.success(comment, "댓글이 성공적으로 등록되었습니다.");
    }

    /**
     * 댓글 소프트 삭제
     * DELETE /api/comments/{commentId}
     */
    @DeleteMapping("/api/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        String dummyUser = "USER_SYSTEM";
        commentService.deleteComment(commentId, dummyUser);
        return ApiResponse.success("댓글이 성공적으로 삭제되었습니다.");
    }
}
