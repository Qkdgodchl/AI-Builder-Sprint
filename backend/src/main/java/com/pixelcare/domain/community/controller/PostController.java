package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostListItemResponse;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.service.PostService;
import com.pixelcare.global.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 커뮤니티 게시글 목록 조회
     * GET /api/posts?category=REVIEW&sort=latest&page=0&size=10
     */
    @GetMapping
    public ApiResponse<Page<PostListItemResponse>> getPosts(
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        Page<PostListItemResponse> posts = postService.getPosts(category, sort, page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 게시글 상세 조회
     * GET /api/posts/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostDetail(@PathVariable Long id) {
        PostResponse post = postService.getPostDetail(id);
        return ApiResponse.success(post);
    }

    /**
     * 게시글 작성
     * POST /api/posts
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(@RequestBody PostCreateRequest request) {
        // TODO: 로그인 시큐리티 적용 후 현재 유저 정보 세팅
        Long dummyAuthorId = 5L;
        String dummyNickname = "따뜻한픽셀";
        String dummyBadge = "LV2_WARMTH";
        PostResponse createdPost = postService.createPost(request, dummyAuthorId, dummyNickname, dummyBadge);
        return ApiResponse.success(createdPost, "게시글이 성공적으로 등록되었습니다.");
    }

    /**
     * 게시글 소프트 삭제
     * DELETE /api/posts/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        String dummyUser = "USER_SYSTEM";
        postService.deletePost(id, dummyUser);
        return ApiResponse.success("게시글이 성공적으로 삭제되었습니다.");
    }
}
