package com.pixelcare.domain.community.dto;

import com.pixelcare.domain.community.entity.Comment;
import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private AuthorResponse author;
    private String content;
    private Long parentCommentId;
    private LocalDateTime createdAt;

    public CommentResponse() {}

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.author = new AuthorResponse(null, comment.getAuthorNickname(), comment.getAuthorBadge());
        this.content = comment.getContent();
        this.parentCommentId = comment.getParentCommentId();
        this.createdAt = comment.getCreatedAt();
    }

    public Long getId() { return id; }
    public AuthorResponse getAuthor() { return author; }
    public String getContent() { return content; }
    public Long getParentCommentId() { return parentCommentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
