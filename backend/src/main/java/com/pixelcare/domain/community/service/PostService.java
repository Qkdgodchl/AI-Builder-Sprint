package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.LikeToggleResponse;
import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostListItemResponse;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostCategory;
import com.pixelcare.domain.community.entity.PostLike;
import com.pixelcare.domain.community.repository.PostLikeRepository;
import com.pixelcare.domain.community.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    public PostService(PostRepository postRepository, PostLikeRepository postLikeRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
    }

    /**
     * 커뮤니티 게시글 목록 조회 (카테고리 필터, 정렬, 페이징)
     */
    public Page<PostListItemResponse> getPosts(String category, String sort, int page, int size) {
        Sort sortOption;
        if ("likes".equalsIgnoreCase(sort)) {
            sortOption = Sort.by(Sort.Direction.DESC, "likeCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            sortOption = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        PageRequest pageRequest = PageRequest.of(page, size, sortOption);

        if (category != null && !"ALL".equalsIgnoreCase(category)) {
            try {
                PostCategory postCategory = PostCategory.valueOf(category.toUpperCase());
                return postRepository.findByCategoryAndIsDeletedFalse(postCategory, pageRequest)
                        .map(PostListItemResponse::new);
            } catch (IllegalArgumentException e) {
                return postRepository.findByIsDeletedFalse(pageRequest)
                        .map(PostListItemResponse::new);
            }
        }

        return postRepository.findByIsDeletedFalse(pageRequest)
                .map(PostListItemResponse::new);
    }

    @Transactional
    public PostResponse getPostDetail(Long id) {
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + id));
        post.incrementViewCount();
        return new PostResponse(post);
    }

    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long authorId, String authorNickname, String authorBadge) {
        PostCategory category = PostCategory.FREE;
        if (request.getCategory() != null) {
            try {
                category = PostCategory.valueOf(request.getCategory().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Post post = new Post(
                authorId,
                authorNickname,
                authorBadge,
                category,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl()
        );
        Post savedPost = postRepository.save(post);
        return new PostResponse(savedPost);
    }

    @Transactional
    public void deletePost(Long id, String deletedBy) {
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 삭제된 게시글입니다. id=" + id));
        post.markDeleted(deletedBy);
    }

    /**
     * 게시글 좋아요 토글 (누르면 +1 / 다시 누르면 -1)
     */
    @Transactional
    public LikeToggleResponse toggleLike(Long postId, Long userId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + postId));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // 이미 좋아요 한 상태 -> 취소 처리
            postLikeRepository.delete(existingLike.get());
            post.updateLikeCount(-1);
            isLiked = false;
        } else {
            // 좋아요 안 한 상태 -> 좋아요 등록
            PostLike postLike = new PostLike(post, userId);
            postLikeRepository.save(postLike);
            post.updateLikeCount(1);
            isLiked = true;
        }

        return new LikeToggleResponse(postId, isLiked, post.getLikeCount());
    }
}
