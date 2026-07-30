package com.pixelcare.domain.user.dto;

public class SignupRequestDto {
    private String email;
    private String password;
    private String nickname;
    private String name;

    public SignupRequestDto() {}

    public SignupRequestDto(String email, String password, String nickname, String name) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }

    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setName(String name) { this.name = name; }
}
