package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.entity.Post;
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

    @Transactional
    public PostResponse getPostDetail(Long id) {
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + id));
        post.incrementViewCount();
        return new PostResponse(post);
    }

    @Transactional
    public PostResponse createPost(PostCreateRequest request, String authorNickname, String authorBadge) {
        Post post = new Post(
                request.getTitle(),
                request.getContent(),
                authorNickname,
                authorBadge,
                request.getCategory(),
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
