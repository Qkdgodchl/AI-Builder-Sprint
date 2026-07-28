package com.pixelcare.community;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<PostResponseDto> getAllPosts(String category) {
        List<Post> posts;
        if (category != null && !category.isBlank()) {
            posts = postRepository.findByCategoryOrderByIdDesc(category.toUpperCase());
        } else {
            posts = postRepository.findAllByOrderByIdDesc();
        }
        return posts.stream()
                .map(PostResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostResponseDto getPostDetail(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
        post.setViews(post.getViews() + 1);
        return PostResponseDto.fromEntity(post);
    }

    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto) {
        String category = (requestDto.getCategory() != null && !requestDto.getCategory().isBlank())
                ? requestDto.getCategory().toUpperCase()
                : "GENERAL";

        Post post = Post.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .author(requestDto.getAuthor())
                .category(category)
                .likes(0)
                .views(0)
                .build();

        Post saved = postRepository.save(post);
        return PostResponseDto.fromEntity(saved);
    }

    @Transactional
    public PostResponseDto likePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + id));
        post.setLikes(post.getLikes() + 1);
        return PostResponseDto.fromEntity(post);
    }
}
