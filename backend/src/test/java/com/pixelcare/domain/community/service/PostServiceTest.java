package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.PostLikeResponse;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostLike;
import com.pixelcare.domain.community.repository.PostLikeRepository;
import com.pixelcare.domain.community.repository.PostRepository;
import com.pixelcare.domain.management.repository.OperatorAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceTest {

    private PostRepository postRepository;
    private PostLikeRepository postLikeRepository;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        postLikeRepository = mock(PostLikeRepository.class);
        postService = new PostService(
                postRepository,
                postLikeRepository,
                mock(OperatorAuditRepository.class)
        );
    }

    @Test
    void repeatedLikeBySameUserTogglesWithoutDuplicatingCount() {
        Post post = new Post(
                "봉사 후기",
                "따뜻한 경험",
                20L,
                "작성자",
                "LV1_SEED",
                "REVIEW",
                null
        );
        PostLike savedLike = new PostLike(3L, 20L);

        when(postRepository.findByIdForLikeUpdate(3L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByPostIdAndUserId(3L, 20L))
                .thenReturn(Optional.empty(), Optional.of(savedLike));

        PostLikeResponse liked = postService.toggleLike(3L, 20L);
        PostLikeResponse unliked = postService.toggleLike(3L, 20L);

        assertThat(liked.isLiked()).isTrue();
        assertThat(liked.likeCount()).isEqualTo(1);
        assertThat(unliked.isLiked()).isFalse();
        assertThat(unliked.likeCount()).isZero();
        assertThat(post.getLikeCount()).isZero();
        verify(postLikeRepository).save(org.mockito.ArgumentMatchers.any(PostLike.class));
        verify(postLikeRepository).delete(savedLike);
    }
}
