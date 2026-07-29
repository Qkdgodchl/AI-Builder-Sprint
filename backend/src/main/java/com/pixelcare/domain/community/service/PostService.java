package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostListItemResponse;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostCategory;
import com.pixelcare.domain.community.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
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
                // 잘지 않은 카테고리 문자열이 올 경우 전체 목록 반환
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
}
