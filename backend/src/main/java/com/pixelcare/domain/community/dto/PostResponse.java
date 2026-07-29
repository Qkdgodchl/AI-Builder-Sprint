package com.pixelcare.domain.community.dto;

import com.pixelcare.domain.community.entity.Post;
import java.time.LocalDateTime;

public class PostResponse {

    private Long id;
    private String category;
    private String title;
    private String content;
    private String authorNickname;
    private String authorBadge;
    private String imageUrl;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private LocalDateTime createdAt;

    public PostResponse() {}

    public PostResponse(Post post) {
        this.id = post.getId();
        this.category = post.getCategory();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.authorNickname = post.getAuthorNickname();
        this.authorBadge = post.getAuthorBadge();
        this.imageUrl = post.getImageUrl();
        this.likeCount = post.getLikeCount();
        this.commentCount = post.getCommentCount();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
    public String getImageUrl() { return imageUrl; }
    public Integer getLikeCount() { return likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
