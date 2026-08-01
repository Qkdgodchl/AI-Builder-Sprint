package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.PostLikeResponse;
import com.pixelcare.domain.community.service.PostService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import com.pixelcare.global.error.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerTest {

    MockMvc mockMvc;
    AuthGuard authGuard;
    PostService postService;
    CurrentUser author;

    @BeforeEach
    void setUp() {
        authGuard = mock(AuthGuard.class);
        postService = mock(PostService.class);
        author = new CurrentUser(20L, "author@example.com", "작성자", Set.of("USER"));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostController(postService, authGuard))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authorCanDeletePost() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);

        mockMvc.perform(delete("/api/posts/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).deletePostAsUser(author, 3L);
    }

    @Test
    void authenticatedUserCanGetOwnPosts() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);
        when(postService.getMyPosts(20L, 0, 20))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/posts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(postService).getMyPosts(20L, 0, 20);
    }

    @Test
    void authenticatedUserCanTogglePostLike() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);
        when(postService.toggleLike(3L, 20L))
                .thenReturn(new PostLikeResponse(3L, true, 1));

        mockMvc.perform(post("/api/posts/3/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postId").value(3))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        verify(postService).toggleLike(3L, 20L);
    }

    @Test
    void otherUserReceives403() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "POST_DELETE_FORBIDDEN", "삭제 권한이 없습니다."))
                .when(postService).deletePostAsUser(author, 3L);

        mockMvc.perform(delete("/api/posts/3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unauthenticatedUserReceives401() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.")
        );

        mockMvc.perform(delete("/api/posts/3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void missingPostReceives404() throws Exception {
        when(authGuard.requireUser(any(HttpServletRequest.class))).thenReturn(author);
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."))
                .when(postService).deletePostAsUser(author, 999L);

        mockMvc.perform(delete("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
