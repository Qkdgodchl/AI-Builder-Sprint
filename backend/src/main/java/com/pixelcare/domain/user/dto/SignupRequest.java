package com.pixelcare.domain.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "비밀번호에는 영문과 숫자가 모두 포함되어야 합니다."
        )
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        String nickname,
        String phone,
        LocalDate birthDate,
        String region,
        List<String> interests,

        @AssertTrue(message = "개인정보 수집·이용 동의가 필요합니다.")
        boolean privacyConsent
) {}
