package com.pixelcare.domain.community.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_nickname", nullable = false)
    private String authorNickname;

    @Column(name = "author_badge", nullable = false)
    private String authorBadge = "LV1_SEED";

    public Comment() {}

    public Comment(Long postId, String content, String authorNickname, String authorBadge) {
        this.postId = postId;
        this.content = content;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge != null ? authorBadge : "LV1_SEED";
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public String getContent() { return content; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
}
