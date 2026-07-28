package com.pixelcare.community;

import java.time.LocalDateTime;

public class PostResponseDto {

    private Long id;
    private String title;
    private String content;
    private String author;
    private String category;
    private Integer likes;
    private Integer views;
    private LocalDateTime createdAt;

    public PostResponseDto() {}

    public PostResponseDto(Long id, String title, String content, String author, String category,
                           Integer likes, Integer views, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category;
        this.likes = likes;
        this.views = views;
        this.createdAt = createdAt;
    }

    public static PostResponseDto fromEntity(Post post) {
        return new PostResponseDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCategory(),
                post.getLikes(),
                post.getViews(),
                post.getCreatedAt()
        );
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

    public static PostResponseDtoBuilder builder() {
        return new PostResponseDtoBuilder();
    }

    public static class PostResponseDtoBuilder {
        private Long id;
        private String title;
        private String content;
        private String author;
        private String category;
        private Integer likes;
        private Integer views;
        private LocalDateTime createdAt;

        public PostResponseDtoBuilder id(Long id) { this.id = id; return this; }
        public PostResponseDtoBuilder title(String title) { this.title = title; return this; }
        public PostResponseDtoBuilder content(String content) { this.content = content; return this; }
        public PostResponseDtoBuilder author(String author) { this.author = author; return this; }
        public PostResponseDtoBuilder category(String category) { this.category = category; return this; }
        public PostResponseDtoBuilder likes(Integer likes) { this.likes = likes; return this; }
        public PostResponseDtoBuilder views(Integer views) { this.views = views; return this; }
        public PostResponseDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PostResponseDto build() {
            return new PostResponseDto(id, title, content, author, category, likes, views, createdAt);
        }
    }
}
