package com.pixelcare.domain.clm.entity;

import com.pixelcare.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clm_documents")
public class ClmDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "volunteer_id", nullable = false)
    private Long volunteerId;

    @Column(name = "volunteer_title", nullable = false)
    private String volunteerTitle;

    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "applicant_email", nullable = false)
    private String applicantEmail;

    @Column(name = "applicant_phone")
    private String applicantPhone;

    @Column(name = "modusign_document_id")
    private String modusignDocumentId;

    @Column(name = "modusign_participant_id")
    private String modusignParticipantId;

    @Column(name = "modusign_template_id")
    private String modusignTemplateId;

    @Column(name = "signing_method", nullable = false)
    private String signingMethod = "SECURE_LINK";

    @Column(name = "signing_url", columnDefinition = "TEXT")
    private String signingUrl;

    @Column(name = "signing_url_expires_at")
    private LocalDateTime signingUrlExpiresAt;

    @Column(nullable = false)
    private String status = "PENDING_SIGNATURE";

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "last_event_type")
    private String lastEventType;

    public ClmDocument() {}

    public ClmDocument(Long volunteerId, String volunteerTitle, Long applicantUserId,
                       String applicantName, String applicantEmail, String applicantPhone,
                       String modusignDocumentId, String modusignParticipantId, String modusignTemplateId,
                       String signingUrl, LocalDateTime signingUrlExpiresAt) {
        this.volunteerId = volunteerId;
        this.volunteerTitle = volunteerTitle;
        this.applicantUserId = applicantUserId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.modusignDocumentId = modusignDocumentId;
        this.modusignParticipantId = modusignParticipantId;
        this.modusignTemplateId = modusignTemplateId;
        this.signingMethod = "SECURE_LINK";
        this.signingUrl = signingUrl;
        this.signingUrlExpiresAt = signingUrlExpiresAt;
        this.status = "PENDING_SIGNATURE";
    }

    public Long getId() { return id; }
    public Long getVolunteerId() { return volunteerId; }
    public String getVolunteerTitle() { return volunteerTitle; }
    public Long getApplicantUserId() { return applicantUserId; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getApplicantPhone() { return applicantPhone; }
    public String getModusignDocumentId() { return modusignDocumentId; }
    public String getModusignParticipantId() { return modusignParticipantId; }
    public String getModusignTemplateId() { return modusignTemplateId; }
    public String getSigningMethod() { return signingMethod; }
    public String getSigningUrl() { return signingUrl; }
    public LocalDateTime getSigningUrlExpiresAt() { return signingUrlExpiresAt; }
    public String getStatus() { return status; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public String getLastEventType() { return lastEventType; }

    public void applyModusignEvent(String eventType) {
        this.lastEventType = eventType;
        switch (eventType) {
            case "document_started" -> this.status = "SIGNING";
            case "document_signed" -> this.status = "PARTIALLY_SIGNED";
            case "document_all_signed" -> {
                this.status = "SIGNED";
                if (this.signedAt == null) this.signedAt = LocalDateTime.now();
            }
            case "document_rejected" -> {
                this.status = "REJECTED";
                if (this.rejectedAt == null) this.rejectedAt = LocalDateTime.now();
            }
            case "document_request_canceled" -> this.status = "CANCELED";
            case "document_signing_canceled" -> this.status = "SIGNING_CANCELED";
            default -> { }
        }
    }

    public void updateSecureLink(String signingUrl, LocalDateTime expiresAt) {
        this.signingUrl = signingUrl;
        this.signingUrlExpiresAt = expiresAt;
    }
}
