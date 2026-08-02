package com.pixelcare.domain.file.repository;

import com.pixelcare.domain.file.dto.StoredFileResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class StoredFileRepository {

    private final JdbcTemplate jdbcTemplate;

    public StoredFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public StoredFileResponse create(
            Long ownerUserId,
            String storageKey,
            String originalName,
            String contentType,
            long size,
            String checksum,
            String purpose
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO stored_files (
                        owner_user_id, storage_key, original_name, content_type,
                        size_bytes, checksum, file_purpose
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, ownerUserId);
            statement.setString(2, storageKey);
            statement.setString(3, originalName);
            statement.setString(4, contentType);
            statement.setLong(5, size);
            statement.setString(6, checksum);
            statement.setString(7, purpose);
            return statement;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        return jdbcTemplate.queryForObject("""
                SELECT id, original_name, content_type, size_bytes, file_purpose, created_at
                FROM stored_files WHERE id = ?
                """, (rs, rowNum) -> new StoredFileResponse(
                rs.getLong("id"),
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getLong("size_bytes"),
                rs.getString("file_purpose"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), id);
    }

    public record StoredFileLocation(String storageKey, String originalName,
                                     String contentType, String purpose) {}

    public java.util.Optional<StoredFileLocation> findLocation(Long id) {
        return jdbcTemplate.query("""
                SELECT storage_key, original_name, content_type, file_purpose
                FROM stored_files WHERE id = ? AND deleted_at IS NULL
                """, (rs, rowNum) -> new StoredFileLocation(
                rs.getString("storage_key"),
                rs.getString("original_name"),
                rs.getString("content_type"),
                rs.getString("file_purpose")
        ), id).stream().findFirst();
    }
}
