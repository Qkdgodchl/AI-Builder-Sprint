package com.pixelcare.domain.community.dto;

import jakarta.validation.constraints.NotBlank;

public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
    private String content;

    private String authorNickname;
    private String authorBadge;

    public CommentCreateRequest() {}

    public CommentCreateRequest(String content, String authorNickname, String authorBadge) {
        this.content = content;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge;
    }

    public String getContent() { return content; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
}
