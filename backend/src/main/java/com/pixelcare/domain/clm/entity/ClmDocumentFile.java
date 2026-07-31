package com.pixelcare.domain.clm.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "clm_document_files",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clm_document_files_document_type",
                columnNames = {"clm_document_id", "file_type"}
        )
)
public class ClmDocumentFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clm_document_id", nullable = false)
    private Long clmDocumentId;

    @Column(name = "file_type", nullable = false, length = 40)
    private String fileType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    protected ClmDocumentFile() {}

    public ClmDocumentFile(Long clmDocumentId, String fileType, String storageKey, String originalName,
                           String contentType, long sizeBytes, String sha256) {
        this.clmDocumentId = clmDocumentId;
        this.fileType = fileType;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    public Long getId() { return id; }
    public Long getClmDocumentId() { return clmDocumentId; }
    public String getFileType() { return fileType; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
}
