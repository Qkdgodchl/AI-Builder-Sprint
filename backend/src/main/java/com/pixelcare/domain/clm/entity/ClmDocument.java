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

    @Column(name = "commitment_id")
    private Long commitmentId;

    @Column(name = "signature_request_id")
    private Long signatureRequestId;

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

    @Column(name = "last_event_rank", nullable = false)
    private int lastEventRank;

    @Column(name = "completion_message", columnDefinition = "TEXT")
    private String completionMessage;

    @Column(name = "completion_message_source")
    private String completionMessageSource;

    @Column(name = "completion_message_created_at")
    private LocalDateTime completionMessageCreatedAt;

    public ClmDocument() {}

    public ClmDocument(Long commitmentId, Long signatureRequestId,
                       Long volunteerId, String volunteerTitle, Long applicantUserId,
                       String applicantName, String applicantEmail, String applicantPhone,
                       String modusignDocumentId, String modusignParticipantId, String modusignTemplateId,
                       String signingUrl, LocalDateTime signingUrlExpiresAt) {
        this.commitmentId = commitmentId;
        this.signatureRequestId = signatureRequestId;
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

    public ClmDocument(Long volunteerId, String volunteerTitle, Long applicantUserId,
                       String applicantName, String applicantEmail, String applicantPhone,
                       String modusignDocumentId, String modusignParticipantId, String modusignTemplateId,
                       String signingUrl, LocalDateTime signingUrlExpiresAt) {
        this(null, null, volunteerId, volunteerTitle, applicantUserId, applicantName, applicantEmail,
                applicantPhone, modusignDocumentId, modusignParticipantId, modusignTemplateId,
                signingUrl, signingUrlExpiresAt);
    }

    public Long getId() { return id; }
    public Long getCommitmentId() { return commitmentId; }
    public Long getSignatureRequestId() { return signatureRequestId; }
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
    public int getLastEventRank() { return lastEventRank; }
    public String getCompletionMessage() { return completionMessage; }
    public String getCompletionMessageSource() { return completionMessageSource; }
    public LocalDateTime getCompletionMessageCreatedAt() { return completionMessageCreatedAt; }

    public boolean hasCompletionMessage() {
        return completionMessage != null && !completionMessage.isBlank();
    }

    /** 서명이 끝난 약정에만 감사 메시지를 남기고, 이미 남겼다면 다시 덮어쓰지 않는다. */
    public boolean applyCompletionMessage(String message, String source) {
        if (message == null || message.isBlank()) return false;
        if (hasCompletionMessage()) return false;
        if (!"SIGNED".equals(status)) return false;
        this.completionMessage = message;
        this.completionMessageSource = source;
        this.completionMessageCreatedAt = LocalDateTime.now();
        return true;
    }

    public boolean applyModusignEvent(String eventType) {
        int incomingRank = eventRank(eventType);
        if (incomingRank < lastEventRank || lastEventRank >= 100) return false;
        this.lastEventType = eventType;
        this.lastEventRank = incomingRank;
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
            default -> { return false; }
        }
        return true;
    }

    private int eventRank(String eventType) {
        return switch (eventType) {
            case "document_started" -> 10;
            case "document_signed" -> 20;
            case "document_signing_canceled" -> 30;
            case "document_all_signed", "document_rejected", "document_request_canceled" -> 100;
            default -> -1;
        };
    }

    public void updateSecureLink(String signingUrl, LocalDateTime expiresAt) {
        this.signingUrl = signingUrl;
        this.signingUrlExpiresAt = expiresAt;
    }
}
