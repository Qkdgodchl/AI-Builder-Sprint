package com.pixelcare.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostCreateRequest {

    @NotBlank(message = "카테고리는 필수 선택 항목입니다. (FREE, REVIEW, QUESTION, RECRUIT)")
    private String category;

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 작성해 주세요.")
    private String title;

    @NotBlank(message = "내용은 필수 입력 항목입니다.")
    @Size(min = 5, message = "내용은 최소 5자 이상 작성해 주세요.")
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
