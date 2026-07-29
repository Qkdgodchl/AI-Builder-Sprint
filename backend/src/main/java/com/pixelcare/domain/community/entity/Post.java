package com.pixelcare.domain.community.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String authorNickname;

    private String authorBadge = "LV1_SEED";

    @Column(nullable = false)
    private String category = "GENERAL";

    private String imageUrl;

    @Column(nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false)
    private Integer commentCount = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    public Post() {}

    public Post(String title, String content, String authorNickname, String authorBadge, String category, String imageUrl) {
        this.title = title;
        this.content = content;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge != null ? authorBadge : "LV1_SEED";
        this.category = category != null ? category : "GENERAL";
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public Integer getLikeCount() { return likeCount; }
    public Integer getCommentCount() { return commentCount; }
    public Integer getViewCount() { return viewCount; }

    public void update(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }
}
