package com.pixelcare.domain.user.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    private String name;

    @Column(nullable = false)
    private String role = "USER"; // USER, CENTER_MANAGER, OPERATOR

    @Column(name = "account_status")
    private String accountStatus = "ACTIVE";

    @Column(name = "privacy_consent_at")
    private LocalDateTime privacyConsentAt;

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
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getAccountStatus() { return accountStatus; }
    public LocalDateTime getPrivacyConsentAt() { return privacyConsentAt; }
    public Double getTemperature() { return temperature; }

    @PrePersist
    public void prePersistUser() {
        if (this.temperature == null) {
            this.temperature = 36.5;
        }
        if (this.accountStatus == null) {
            this.accountStatus = "ACTIVE";
        }
        if (this.role == null) {
            this.role = "USER";
        }
    }

    public void updateRole(String newRole) {
        this.role = newRole;
    }

    public void addTemperature(double delta) {
        this.temperature = Math.round((this.temperature + delta) * 10.0) / 10.0;
    }
}
