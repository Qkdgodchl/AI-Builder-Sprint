package com.pixelcare.domain.community.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String authorNickname;

    private String authorBadge = "LV1_SEED";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Long parentCommentId;

    public Comment() {}

    public Comment(Post post, String authorNickname, String authorBadge, String content, Long parentCommentId) {
        this.post = post;
        this.authorNickname = authorNickname;
        this.authorBadge = authorBadge != null ? authorBadge : "LV1_SEED";
        this.content = content;
        this.parentCommentId = parentCommentId;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorBadge() { return authorBadge; }
    public String getContent() { return content; }
    public Long getParentCommentId() { return parentCommentId; }
}
