package com.pixelcare.domain.community.dto;

import com.pixelcare.domain.community.entity.Comment;

import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private Long postId;
    private String content;
    private String authorNickname;
    private String authorBadge;
    private LocalDateTime createdAt;

    public CommentResponse() {}

    public CommentResponse(Long id, Long postId, String content, String authorNickname, String authorBadge, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.content = content;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge;
        this.createdAt = createdAt;
    }

    public static CommentResponse fromEntity(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getContent(),
                comment.getAuthorNickname(),
                comment.getAuthorBadge(),
                comment.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public String getContent() { return content; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
