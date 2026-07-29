package com.pixelcare.domain.community.dto;

public class PostCreateRequest {

    private String category;
    private String title;
    private String content;
    private String imageUrl;

    public PostCreateRequest() {}

    public PostCreateRequest(String category, String title, String content, String imageUrl) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
}
