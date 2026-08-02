package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostLikeResponse;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostLike;
import com.pixelcare.domain.community.repository.PostLikeRepository;
import com.pixelcare.domain.community.repository.PostRepository;
import com.pixelcare.domain.management.repository.OperatorAuditRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final OperatorAuditRepository auditRepository;

    public PostService(
            PostRepository postRepository,
            PostLikeRepository postLikeRepository,
            OperatorAuditRepository auditRepository
    ) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.auditRepository = auditRepository;
    }

    public Page<PostResponse> getPosts(String category, String sort, int page, int size) {
        Sort sortOption = "likes".equalsIgnoreCase(sort) 
                ? Sort.by(Sort.Direction.DESC, "likeCount") 
                : Sort.by(Sort.Direction.DESC, "createdAt");

        PageRequest pageRequest = PageRequest.of(page, size, sortOption);

        if (category != null && !"ALL".equalsIgnoreCase(category)) {
            return postRepository.findByCategoryAndIsDeletedFalse(category, pageRequest)
                    .map(PostResponse::new);
        }
        return postRepository.findByIsDeletedFalse(pageRequest)
                .map(PostResponse::new);
    }

    public Page<PostResponse> getPostsForOperator(int page, int size) {
        return postRepository.findByIsDeletedFalse(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(PostResponse::new);
    }

    public Page<PostResponse> getMyPosts(Long userId, int page, int size) {
        return postRepository.findByAuthorUserIdAndIsDeletedFalse(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(PostResponse::new);
    }

    @Transactional
    public PostResponse getPostDetail(Long id) {
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + id));
        post.incrementViewCount();
        return new PostResponse(post);
    }

    @Transactional
    public PostLikeResponse toggleLike(Long id, Long userId) {
        Post post = postRepository.findByIdForLikeUpdate(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_NOT_FOUND",
                        "존재하지 않거나 삭제된 게시글입니다."
                ));

        boolean liked;
        var existingLike = postLikeRepository.findByPostIdAndUserId(id, userId);
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.updateLikeCount(-1);
            liked = false;
        } else {
            postLikeRepository.save(new PostLike(id, userId));
            post.updateLikeCount(1);
            liked = true;
        }

        return new PostLikeResponse(id, liked, post.getLikeCount());
    }

    @Transactional
    public PostResponse createPost(
            PostCreateRequest request,
            Long authorUserId,
            String authorNickname,
            String authorBadge
    ) {
        Post post = new Post(
                request.getTitle(),
                request.getContent(),
                authorUserId,
                authorNickname,
                authorBadge,
                request.getCategory(),
                request.getImageUrl()
        );
        Post savedPost = postRepository.save(post);
        return new PostResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long operatorId, Long id, PostCreateRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + id));
        post.update(request.getCategory(), request.getTitle(), request.getContent(), request.getImageUrl());
        auditRepository.record(operatorId, "COMMUNITY_POST_UPDATE", "COMMUNITY_POST", id.toString());
        return new PostResponse(post);
    }

    @Transactional
    public void deletePostAsUser(CurrentUser user, Long id) {
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_NOT_FOUND",
                        "존재하지 않거나 이미 삭제된 게시글입니다."
                ));
        boolean operator = user.hasRole("OPERATOR");
        boolean author = post.getAuthorUserId() != null && post.getAuthorUserId().equals(user.id());
        if (!operator && !author) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "POST_DELETE_FORBIDDEN",
                    "게시글 작성자 또는 운영진만 삭제할 수 있습니다."
            );
        }
        post.markDeleted((operator ? "OPERATOR:" : "AUTHOR:") + user.id());
        if (operator) {
            auditRepository.record(user.id(), "COMMUNITY_POST_DELETE", "COMMUNITY_POST", id.toString());
        }
    }
}
