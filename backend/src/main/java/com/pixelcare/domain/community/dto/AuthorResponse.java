package com.pixelcare.domain.community.dto;

public class AuthorResponse {

    private Long id;
    private String nickname;
    private String badge;

    public AuthorResponse() {}

    public AuthorResponse(Long id, String nickname, String badge) {
        this.id = id;
        this.nickname = nickname;
        this.badge = badge;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public String getBadge() { return badge; }
}
