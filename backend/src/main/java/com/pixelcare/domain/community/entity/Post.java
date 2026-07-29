package com.pixelcare.domain.community.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_nickname", nullable = false)
    private String authorNickname;

    @Column(name = "author_badge", nullable = false)
    private String authorBadge = "LV1_SEED";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category = PostCategory.FREE;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    public Post() {}

    public Post(Long authorId, String authorNickname, String authorBadge, PostCategory category, String title, String content, String imageUrl) {
        this.authorId = authorId != null ? authorId : 1L;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge != null ? authorBadge : "LV1_SEED";
        this.category = category != null ? category : PostCategory.FREE;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.likeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
    }

    public Post(Long authorId, String authorNickname, String authorBadge, PostCategory category, String title, String content, String imageUrl, Integer likeCount, Integer commentCount, Integer viewCount) {
        this.authorId = authorId != null ? authorId : 1L;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge != null ? authorBadge : "LV1_SEED";
        this.category = category != null ? category : PostCategory.FREE;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.commentCount = commentCount != null ? commentCount : 0;
        this.viewCount = viewCount != null ? viewCount : 0;
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
    public PostCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public Integer getLikeCount() { return likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public Integer getViewCount() { return viewCount; }

    public String getContentSnippet() {
        if (content == null) return "";
        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }

    public void update(String title, String content, PostCategory category, String imageUrl) {
        this.title = title;
        this.content = content;
        if (category != null) this.category = category;
        this.imageUrl = imageUrl;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void updateCommentCount(int delta) {
        this.commentCount = Math.max(0, this.commentCount + delta);
    }
}
