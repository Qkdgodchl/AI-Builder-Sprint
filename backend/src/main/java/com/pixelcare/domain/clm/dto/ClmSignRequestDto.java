package com.pixelcare.domain.clm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClmSignRequestDto {

    @NotNull(message = "봉사/기부 공고 ID는 필수입니다.")
    private Long volunteerId;

    @NotBlank(message = "신청자 이름은 필수 입력 항목입니다.")
    private String applicantName;

    @NotBlank(message = "신청자 이메일은 필수 입력 항목입니다.")
    private String applicantEmail;

    private String applicantPhone;

    public ClmSignRequestDto() {}

    public ClmSignRequestDto(Long volunteerId, String applicantName, String applicantEmail, String applicantPhone) {
        this.volunteerId = volunteerId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
    }

    public Long getVolunteerId() { return volunteerId; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getApplicantPhone() { return applicantPhone; }
}
