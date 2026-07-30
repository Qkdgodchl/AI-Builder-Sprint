package com.pixelcare.domain.user.dto;

import com.pixelcare.domain.user.entity.User;

public class UserResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private Double temperature;

    public UserResponseDto() {}

    public UserResponseDto(Long id, String email, String nickname, String role, Double temperature) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.temperature = temperature;
    }

    public static UserResponseDto fromEntity(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getTemperature()
        );
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getRole() { return role; }
    public Double getTemperature() { return temperature; }
}
