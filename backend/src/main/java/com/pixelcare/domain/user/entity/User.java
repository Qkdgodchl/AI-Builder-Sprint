package com.pixelcare.domain.user.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String role = "USER"; // USER, CENTER_MANAGER, OPERATOR

    @Column(nullable = false)
    private Double temperature = 36.5;

    public User() {}

    public User(String email, String nickname, String role) {
        this.email = email;
        this.nickname = nickname;
        this.role = role != null ? role : "USER";
        this.temperature = 36.5;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getRole() { return role; }
    public Double getTemperature() { return temperature; }

    public void updateRole(String newRole) {
        this.role = newRole;
    }

    public void addTemperature(double delta) {
        this.temperature = Math.round((this.temperature + delta) * 10.0) / 10.0;
    }
}
