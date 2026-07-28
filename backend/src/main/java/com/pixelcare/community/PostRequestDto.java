package com.pixelcare.community;

import jakarta.validation.constraints.NotBlank;

public class PostRequestDto {

    @NotBlank(message = "게시글 제목은 필수 입력 항목입니다.")
    private String title;

    @NotBlank(message = "게시글 내용은 필수 입력 항목입니다.")
    private String content;

    @NotBlank(message = "작성자 닉네임은 필수 입력 항목입니다.")
    private String author;

    private String category;

    public PostRequestDto() {}

    public PostRequestDto(String title, String content, String author, String category) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
