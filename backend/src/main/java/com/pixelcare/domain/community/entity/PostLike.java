package com.pixelcare.domain.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_likes_post_user",
                columnNames = {"post_id", "user_id"}
        )
)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PostLike() {}

    public PostLike(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
