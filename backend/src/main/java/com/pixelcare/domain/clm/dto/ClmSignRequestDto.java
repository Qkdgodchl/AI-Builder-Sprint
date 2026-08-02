package com.pixelcare.domain.clm.dto;

import jakarta.validation.constraints.NotBlank;

public class ClmSignRequestDto {

    @NotBlank(message = "약정서 공개 ID는 필수입니다.")
    private String commitmentPublicId;

    private String applicantPhone;

    public ClmSignRequestDto() {}

    public ClmSignRequestDto(String commitmentPublicId, String applicantPhone) {
        this.commitmentPublicId = commitmentPublicId;
        this.applicantPhone = applicantPhone;
    }

    public String getCommitmentPublicId() { return commitmentPublicId; }
    public String getApplicantPhone() { return applicantPhone; }
}
