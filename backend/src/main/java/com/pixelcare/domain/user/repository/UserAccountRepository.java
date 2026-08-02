package com.pixelcare.domain.user.repository;

import com.pixelcare.domain.user.dto.SignupRequest;
import com.pixelcare.domain.user.dto.UserProfileResponse;
import com.pixelcare.global.auth.AuthRepository;
import com.pixelcare.global.common.KeyExtractUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final AuthRepository authRepository;

    public UserAccountRepository(
            JdbcTemplate jdbcTemplate,
            JdbcClient jdbcClient,
            AuthRepository authRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcClient = jdbcClient;
        this.authRepository = authRepository;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    public Long create(SignupRequest request, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? request.name()
                : request.nickname().trim();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO users (
                        email, password_hash, nickname, name, phone, birth_date, region,
                        role, account_status, privacy_consent_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'USER', 'ACTIVE', ?)
                    """, new String[] { "id" });
            statement.setString(1, request.email().trim().toLowerCase());
            statement.setString(2, passwordHash);
            statement.setString(3, nickname);
            statement.setString(4, request.name().trim());
            statement.setString(5, request.phone());
            statement.setDate(6, request.birthDate() == null ? null : Date.valueOf(request.birthDate()));
            statement.setString(7, request.region());
            statement.setObject(8, LocalDateTime.now());
            return statement;
        }, keyHolder);
        Long userId = KeyExtractUtils.extractId(keyHolder);
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, 'USER')",
                userId
        );
        replaceInterests(userId, request.interests());
        return userId;
    }

    public Optional<UserCredential> findCredentialByEmail(String email) {
        return jdbcClient.sql("""
                        SELECT id, email, password_hash, nickname, account_status
                        FROM users
                        WHERE LOWER(email) = LOWER(?) AND is_deleted = FALSE
                        """)
                .param(email)
                .query((rs, rowNum) -> new UserCredential(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("nickname"),
                        rs.getString("account_status")
                ))
                .optional();
    }

    public Optional<UserProfileResponse> findProfile(Long userId) {
        return jdbcClient.sql("""
                        SELECT id, email, nickname, name, phone, birth_date, region,
                               account_status, temperature, created_at
                        FROM users
                        WHERE id = ? AND is_deleted = FALSE
                        """)
                .param(userId)
                .query((rs, rowNum) -> new UserProfileResponse(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toLocalDate(),
                        rs.getString("region"),
                        authRepository.findRoles(userId),
                        findInterests(userId),
                        rs.getString("account_status"),
                        rs.getDouble("temperature"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ))
                .optional();
    }

    public void updateLastLogin(Long userId) {
        jdbcTemplate.update(
                "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?",
                userId
        );
    }

    public void updateProfile(
            Long userId,
            String nickname,
            String phone,
            String region,
            List<String> interests
    ) {
        jdbcTemplate.update("""
                UPDATE users
                SET nickname = COALESCE(?, nickname),
                    phone = COALESCE(?, phone),
                    region = COALESCE(?, region),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, blankToNull(nickname), blankToNull(phone), blankToNull(region), userId);
        if (interests != null) {
            replaceInterests(userId, interests);
        }
    }

    public void grantRole(Long userId, String role) {
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM user_roles WHERE user_id = ? AND role = ?
                )
                """, userId, role, userId, role);
        jdbcTemplate.update(
                "UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                role,
                userId
        );
    }

    private void replaceInterests(Long userId, List<String> interests) {
        jdbcTemplate.update("DELETE FROM user_interests WHERE user_id = ?", userId);
        if (interests == null) {
            return;
        }
        interests.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase())
                .distinct()
                .forEach(value -> jdbcTemplate.update(
                        "INSERT INTO user_interests (user_id, interest_code) VALUES (?, ?)",
                        userId,
                        value
                ));
    }

    private List<String> findInterests(Long userId) {
        return jdbcTemplate.query(
                "SELECT interest_code FROM user_interests WHERE user_id = ? ORDER BY id",
                (rs, rowNum) -> rs.getString("interest_code"),
                userId
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record UserCredential(
            Long id,
            String email,
            String passwordHash,
            String nickname,
            String accountStatus
    ) {}
}
