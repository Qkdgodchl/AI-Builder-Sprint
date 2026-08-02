package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.ai.service.UpstageApiClient;
import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmCommitmentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 전자서명이 끝난 약정에 "마음을 남기는" 한마디를 붙인다.
 * Solar LLM이 약정 내용을 사람 말로 되돌려주고, 호출이 불가능하면 동일한 톤의 로컬 문구로 대체한다.
 */
@Service
public class ClmCompletionMessageService {

    private final ClmCommitmentRepository commitmentRepository;
    private final UpstageApiClient upstageApiClient;

    public ClmCompletionMessageService(
            ClmCommitmentRepository commitmentRepository,
            UpstageApiClient upstageApiClient
    ) {
        this.commitmentRepository = commitmentRepository;
        this.upstageApiClient = upstageApiClient;
    }

    /** 이미 메시지가 있거나 약정 정보를 찾을 수 없으면 조용히 넘어간다. */
    public void attachTo(ClmDocument document) {
        if (document.hasCompletionMessage()) return;
        commitmentRepository.findGratitudeContext(document.getCommitmentId()).ifPresent(context -> {
            String summary = summarize(context, document.getApplicantName());
            upstageApiClient.generateGratitudeMessage(summary)
                    .ifPresentOrElse(
                            message -> document.applyCompletionMessage(message, "UPSTAGE_SOLAR"),
                            () -> document.applyCompletionMessage(fallback(context), "LOCAL_FALLBACK")
                    );
        });
    }

    private String summarize(ClmCommitmentRepository.GratitudeContext context, String fallbackName) {
        StringBuilder summary = new StringBuilder();
        summary.append("약정자: ").append(displayName(context.applicantName(), fallbackName)).append('\n');
        summary.append("약정명: ").append(context.title()).append('\n');
        summary.append("받는 곳: ").append(context.organizationName()).append('\n');
        summary.append("약정 종류: ").append(typeLabel(context.commitmentType())).append('\n');
        if (context.pledgeAmount() != null && context.pledgeAmount().signum() > 0) {
            summary.append("약정 금액: ").append(money(context.pledgeAmount())).append("원\n");
        }
        summary.append("약정 주기: ").append(frequencyLabel(context.pledgeFrequency()));
        return summary.toString();
    }

    private String fallback(ClmCommitmentRepository.GratitudeContext context) {
        String amount = context.pledgeAmount() != null && context.pledgeAmount().signum() > 0
                ? money(context.pledgeAmount()) + "원의 "
                : "";
        return "%s님의 %s약정이 %s에 안전하게 전달되었습니다. 오늘 남기신 서명이 %s의 내일을 바꾸는 첫 장면이 됩니다."
                .formatted(
                        context.applicantName(),
                        amount,
                        context.organizationName(),
                        context.organizationName()
                );
    }

    private String displayName(String name, String fallbackName) {
        if (name != null && !name.isBlank()) return name;
        return fallbackName == null || fallbackName.isBlank() ? "약정자" : fallbackName;
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount.longValue());
    }

    private String typeLabel(String commitmentType) {
        if (commitmentType == null) return "기부·봉사 약정";
        return switch (commitmentType) {
            case "DONATION" -> "기부 약정";
            case "HOMETOWN_DONATION" -> "고향사랑기부 약정";
            case "VOLUNTEER" -> "봉사 약정";
            case "LEGACY_DONATION" -> "유산기부 약정";
            case "CULTURAL_HERITAGE_DONATION" -> "문화유산 후원 약정";
            default -> "기부·봉사 약정";
        };
    }

    private String frequencyLabel(String frequency) {
        if (frequency == null) return "해당 없음";
        return switch (frequency) {
            case "ONE_TIME" -> "일시";
            case "MONTHLY" -> "매월 정기";
            case "ANNUAL" -> "매년 정기";
            default -> "해당 없음";
        };
    }
}
