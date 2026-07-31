package com.pixelcare.domain.clm.dto;

import com.pixelcare.domain.clm.entity.ClmDocument;

import java.time.LocalDateTime;

public class ClmDocumentResponseDto {

    private Long id;
    private Long volunteerId;
    private String volunteerTitle;
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String modusignDocumentId;
    private String signingMethod;
    private String signingUrl;
    private LocalDateTime signingUrlExpiresAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime signedAt;

    public ClmDocumentResponseDto() {}

    public ClmDocumentResponseDto(Long id, Long volunteerId, String volunteerTitle,
                                  String applicantName, String applicantEmail, String applicantPhone,
                                  String modusignDocumentId, String signingMethod, String signingUrl,
                                  LocalDateTime signingUrlExpiresAt, String status,
                                  LocalDateTime createdAt, LocalDateTime signedAt) {
        this.id = id;
        this.volunteerId = volunteerId;
        this.volunteerTitle = volunteerTitle;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.modusignDocumentId = modusignDocumentId;
        this.signingMethod = signingMethod;
        this.signingUrl = signingUrl;
        this.signingUrlExpiresAt = signingUrlExpiresAt;
        this.status = status;
        this.createdAt = createdAt;
        this.signedAt = signedAt;
    }

    public static ClmDocumentResponseDto fromEntity(ClmDocument doc) {
        return new ClmDocumentResponseDto(
                doc.getId(),
                doc.getVolunteerId(),
                doc.getVolunteerTitle(),
                doc.getApplicantName(),
                doc.getApplicantEmail(),
                doc.getApplicantPhone(),
                doc.getModusignDocumentId(),
                doc.getSigningMethod(),
                doc.getSigningUrl(),
                doc.getSigningUrlExpiresAt(),
                doc.getStatus(),
                doc.getCreatedAt(),
                doc.getSignedAt()
        );
    }

    public Long getId() { return id; }
    public Long getVolunteerId() { return volunteerId; }
    public String getVolunteerTitle() { return volunteerTitle; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getApplicantPhone() { return applicantPhone; }
    public String getModusignDocumentId() { return modusignDocumentId; }
    public String getSigningMethod() { return signingMethod; }
    public String getSigningUrl() { return signingUrl; }
    public LocalDateTime getSigningUrlExpiresAt() { return signingUrlExpiresAt; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSignedAt() { return signedAt; }
}
