package com.pixelcare.domain.community.dto;

import com.pixelcare.domain.community.entity.Post;
import java.time.LocalDateTime;

public class PostListItemResponse {

    private Long id;
    private AuthorResponse author;
    private String category;
    private String title;
    private String contentSnippet;
    private String imageUrl;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;

    public PostListItemResponse() {}

    public PostListItemResponse(Post post) {
        this.id = post.getId();
        this.author = new AuthorResponse(post.getAuthorId(), post.getAuthorNickname(), post.getAuthorBadge());
        this.category = post.getCategory().name();
        this.title = post.getTitle();
        this.contentSnippet = post.getContentSnippet();
        this.imageUrl = post.getImageUrl();
        this.likeCount = post.getLikeCount();
        this.commentCount = post.getCommentCount();
        this.createdAt = post.getCreatedAt();
    }

    public Long getId() { return id; }
    public AuthorResponse getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContentSnippet() { return contentSnippet; }
    public String getImageUrl() { return imageUrl; }
    public Integer getLikeCount() { return likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
