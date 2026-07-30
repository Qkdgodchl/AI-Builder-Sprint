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

    @Column(name = "signing_url", columnDefinition = "TEXT")
    private String signingUrl;

    @Column(nullable = false)
    private String status = "PENDING_SIGNATURE";

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    public ClmDocument() {}

    public ClmDocument(Long volunteerId, String volunteerTitle, Long applicantUserId,
                       String applicantName, String applicantEmail, String applicantPhone,
                       String modusignDocumentId, String signingUrl) {
        this.volunteerId = volunteerId;
        this.volunteerTitle = volunteerTitle;
        this.applicantUserId = applicantUserId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.modusignDocumentId = modusignDocumentId;
        this.signingUrl = signingUrl;
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
    public String getSigningUrl() { return signingUrl; }
    public String getStatus() { return status; }
    public LocalDateTime getSignedAt() { return signedAt; }

    public void updateStatusToSigned() {
        this.status = "SIGNED";
        this.signedAt = LocalDateTime.now();
    }

    public void updateSigningInfo(String modusignDocumentId, String signingUrl) {
        this.modusignDocumentId = modusignDocumentId;
        this.signingUrl = signingUrl;
    }
}
