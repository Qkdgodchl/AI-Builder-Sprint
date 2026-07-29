package com.pixelcare.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
    @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 작성해 주세요.")
    private String content;

    private Long parentCommentId;

    public CommentCreateRequest() {}

    public CommentCreateRequest(String content, Long parentCommentId) {
        this.content = content;
        this.parentCommentId = parentCommentId;
    }

    public String getContent() { return content; }
    public Long getParentCommentId() { return parentCommentId; }
}
