package com.pixelcare.domain.community.controller;

import com.pixelcare.domain.community.dto.CommentCreateRequest;
import com.pixelcare.domain.community.dto.CommentResponse;
import com.pixelcare.domain.community.service.CommentService;
import com.pixelcare.global.auth.AuthGuard;
import com.pixelcare.global.auth.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerTest {

    private MockMvc mockMvc;
    private AuthGuard authGuard;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        authGuard = mock(AuthGuard.class);
        commentService = mock(CommentService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentService, authGuard))
                .build();
    }

    @Test
    void authenticatedCommentUsesAccountNicknameInsteadOfRequestNickname() throws Exception {
        CurrentUser user = new CurrentUser(20L, "user@example.com", "전설", Set.of("USER"));
        when(authGuard.resolveUser(any(HttpServletRequest.class))).thenReturn(user);
        when(commentService.createComment(eq(1L), any(CommentCreateRequest.class), eq("전설"), eq("LV2_WARMTH")))
                .thenReturn(new CommentResponse(5L, 1L, "좋아요", "전설", "LV2_WARMTH", LocalDateTime.now()));

        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"좋아요\",\"authorNickname\":\"익명 용사\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorNickname").value("전설"));

        ArgumentCaptor<CommentCreateRequest> requestCaptor = ArgumentCaptor.forClass(CommentCreateRequest.class);
        verify(commentService).createComment(eq(1L), requestCaptor.capture(), eq("전설"), eq("LV2_WARMTH"));
        assertThat(requestCaptor.getValue().getAuthorNickname()).isEqualTo("전설");
    }
}
