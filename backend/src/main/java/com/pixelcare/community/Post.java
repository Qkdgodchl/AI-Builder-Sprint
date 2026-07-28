package com.pixelcare.community;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String category = "GENERAL";

    @Column(nullable = false)
    private Integer likes = 0;

    @Column(nullable = false)
    private Integer views = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Post() {}

    public Post(Long id, String title, String content, String author, String category, Integer likes, Integer views, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category != null ? category : "GENERAL";
        this.likes = likes != null ? likes : 0;
        this.views = views != null ? views : 0;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.likes == null) {
            this.likes = 0;
        }
        if (this.views == null) {
            this.views = 0;
        }
        if (this.category == null) {
            this.category = "GENERAL";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public static class PostBuilder {
        private Long id;
        private String title;
        private String content;
        private String author;
        private String category = "GENERAL";
        private Integer likes = 0;
        private Integer views = 0;
        private LocalDateTime createdAt;

        public PostBuilder id(Long id) { this.id = id; return this; }
        public PostBuilder title(String title) { this.title = title; return this; }
        public PostBuilder content(String content) { this.content = content; return this; }
        public PostBuilder author(String author) { this.author = author; return this; }
        public PostBuilder category(String category) { this.category = category; return this; }
        public PostBuilder likes(Integer likes) { this.likes = likes; return this; }
        public PostBuilder views(Integer views) { this.views = views; return this; }
        public PostBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Post build() {
            return new Post(id, title, content, author, category, likes, views, createdAt);
        }
    }
}
