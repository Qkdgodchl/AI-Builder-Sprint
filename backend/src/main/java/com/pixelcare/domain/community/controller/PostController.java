package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostLikeResponse;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.service.PostService;
import com.pixelcare.global.common.ApiResponse;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final AuthGuard authGuard;

    public PostController(PostService postService, AuthGuard authGuard) {
        this.postService = postService;
        this.authGuard = authGuard;
    }

    @GetMapping
    public ApiResponse<Page<PostResponse>> getPosts(
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        Page<PostResponse> posts = postService.getPosts(category, sort, page, size);
        return ApiResponse.success(posts);
    }

    @GetMapping("/me")
    public ApiResponse<Page<PostResponse>> getMyPosts(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(postService.getMyPosts(user.id(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostDetail(@PathVariable Long id) {
        PostResponse post = postService.getPostDetail(id);
        return ApiResponse.success(post);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<PostLikeResponse> toggleLike(HttpServletRequest request, @PathVariable Long id) {
        CurrentUser user = authGuard.requireUser(request);
        return ApiResponse.success(postService.toggleLike(id, user.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            HttpServletRequest httpRequest,
            @RequestBody PostCreateRequest request
    ) {
        CurrentUser user = authGuard.requireUser(httpRequest);
        String badge = user.hasRole("OPERATOR") ? "OPERATOR" : "LV1_SEED";
        PostResponse createdPost = postService.createPost(
                request,
                user.id(),
                user.nickname(),
                badge
        );
        return ApiResponse.success(createdPost, "게시글이 성공적으로 등록되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(HttpServletRequest request, @PathVariable Long id) {
        CurrentUser user = authGuard.requireUser(request);
        postService.deletePostAsUser(user, id);
        return ApiResponse.success("게시글이 성공적으로 삭제되었습니다.");
    }
}
