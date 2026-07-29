package com.pixelcare.domain.community;

import com.pixelcare.domain.community.dto.PostCreateRequest;
import com.pixelcare.domain.community.dto.PostResponse;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostCategory;
import com.pixelcare.domain.community.repository.PostRepository;
import com.pixelcare.domain.community.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("게시글 작성 및 소프트 삭제(Soft Delete) 기능 검증")
    void deletePost_softDeleteSuccess() {
        // given
        PostCreateRequest request = new PostCreateRequest(
                "REVIEW",
                "테스트 봉사 후기 게시글",
                "테스트 봉사 후기 내용입니다.",
                null
        );
        PostResponse createdPost = postService.createPost(request, 1L, "테스트용사", "LV1_SEED");

        // when
        postService.deletePost(createdPost.getId(), "테스트용사");

        // then
        // 1. 일반 조회 API에서는 예외 발생 (조회 불가능)
        assertThatThrownBy(() -> postService.getPostDetail(createdPost.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않거나 삭제된 게시글입니다.");

        // 2. DB 상에는 레코드가 유지되고 isDeleted = true 로 소프트 삭제됨
        Post dbPost = postRepository.findById(createdPost.getId()).orElseThrow();
        assertThat(dbPost.isDeleted()).isTrue();
        assertThat(dbPost.getDeletedBy()).isEqualTo("테스트용사");
        assertThat(dbPost.getDeletedAt()).isNotNull();
    }
}
