package com.pixelcare.global.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAccessToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO access_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """, userId, tokenHash, Timestamp.valueOf(expiresAt));
    }

    public void saveRefreshToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """, userId, tokenHash, Timestamp.valueOf(expiresAt));
    }

    public Optional<CurrentUser> findUserByAccessTokenHash(String tokenHash) {
        List<CurrentUser> users = jdbcTemplate.query("""
                SELECT u.id, u.email, u.nickname
                FROM access_tokens t
                JOIN users u ON u.id = t.user_id
                WHERE t.token_hash = ?
                  AND t.revoked_at IS NULL
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND u.account_status = 'ACTIVE'
                  AND u.is_deleted = FALSE
                """, (rs, rowNum) -> new CurrentUser(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("nickname"),
                findRoles(rs.getLong("id"))
        ), tokenHash);
        return users.stream().findFirst();
    }

    public Optional<Long> findUserIdByRefreshTokenHash(String tokenHash) {
        List<Long> ids = jdbcTemplate.query("""
                SELECT u.id
                FROM refresh_tokens t
                JOIN users u ON u.id = t.user_id
                WHERE t.token_hash = ?
                  AND t.revoked_at IS NULL
                  AND t.expires_at > CURRENT_TIMESTAMP
                  AND u.account_status = 'ACTIVE'
                  AND u.is_deleted = FALSE
                """, (rs, rowNum) -> rs.getLong("id"), tokenHash);
        return ids.stream().findFirst();
    }

    public Optional<CurrentUser> findCurrentUserById(Long userId) {
        List<CurrentUser> users = jdbcTemplate.query("""
                SELECT id, email, nickname
                FROM users
                WHERE id = ? AND account_status = 'ACTIVE' AND is_deleted = FALSE
                """, (rs, rowNum) -> new CurrentUser(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("nickname"),
                findRoles(rs.getLong("id"))
        ), userId);
        return users.stream().findFirst();
    }

    public Set<String> findRoles(Long userId) {
        Set<String> roles = new HashSet<>(jdbcTemplate.query(
                "SELECT role FROM user_roles WHERE user_id = ?",
                (rs, rowNum) -> rs.getString("role"),
                userId
        ));
        if (roles.isEmpty()) {
            jdbcTemplate.query(
                    "SELECT role FROM users WHERE id = ?",
                    (rs, rowNum) -> rs.getString("role"),
                    userId
            ).stream().findFirst().ifPresent(roles::add);
        }
        return Set.copyOf(roles);
    }

    public void revokeAccessToken(String tokenHash) {
        jdbcTemplate.update("""
                UPDATE access_tokens
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL
                """, tokenHash);
    }

    public void revokeRefreshToken(String tokenHash) {
        jdbcTemplate.update("""
                UPDATE refresh_tokens
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE token_hash = ? AND revoked_at IS NULL
                """, tokenHash);
    }

    public void revokeAllUserTokens(Long userId) {
        jdbcTemplate.update("""
                UPDATE access_tokens SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND revoked_at IS NULL
                """, userId);
        jdbcTemplate.update("""
                UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND revoked_at IS NULL
                """, userId);
    }
}
