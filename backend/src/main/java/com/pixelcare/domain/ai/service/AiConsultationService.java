package com.pixelcare.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.domain.ai.dto.ConsultationResponse;
import com.pixelcare.domain.ai.dto.PledgeIntent;
import com.pixelcare.domain.ai.repository.AiConsultationRepository;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiConsultationService {

    private static final Pattern AMOUNT = Pattern.compile("(\\d[\\d,]*)\\s*(만원|원)");
    private final AiConsultationRepository repository;
    private final UpstageApiClient upstageApiClient;
    private final ObjectMapper objectMapper;

    public AiConsultationService(AiConsultationRepository repository,
                                 UpstageApiClient upstageApiClient,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.upstageApiClient = upstageApiClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConsultationResponse start(Long userId, String message) {
        Long id = repository.create(userId);
        repository.addMessage(id, "USER", message, null);
        return structureAndSave(id, userId, message, null);
    }

    @Transactional
    public ConsultationResponse addMessage(Long id, Long userId, String message) {
        Map<String, Object> row = requireOwned(id, userId);
        repository.addMessage(id, "USER", message, null);
        return structureAndSave(id, userId, message, string(row.get("extracted_preferences_json")));
    }

    public ConsultationResponse get(Long id, Long userId) {
        return response(requireOwned(id, userId), "저장된 약정 의사를 불러왔습니다.", "STORED");
    }

    @Transactional
    public ConsultationResponse update(Long id, Long userId, PledgeIntent intent) {
        requireOwned(id, userId);
        PledgeIntent normalized = withMissingFields(intent);
        String summary = summarize(normalized);
        repository.updateIntent(id, userId, summary, json(normalized));
        repository.addMessage(id, "ASSISTANT", summary, json(normalized));
        return get(id, userId);
    }

    @Transactional
    public ConsultationResponse confirm(Long id, Long userId) {
        ConsultationResponse current = get(id, userId);
        if (current.intent() == null || !current.intent().missingFields().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLEDGE_INTENT_INCOMPLETE",
                    "필수 항목을 모두 확인한 뒤 약정 의사를 확정해주세요.");
        }
        repository.confirm(id, userId);
        return get(id, userId);
    }

    private ConsultationResponse structureAndSave(Long id, Long userId, String message, String previousJson) {
        UpstageApiClient.StructuredIntent result = upstageApiClient
                .structurePledgeIntent(message, previousJson)
                .orElseGet(() -> new UpstageApiClient.StructuredIntent(fallback(message, previousJson), "RULE_FALLBACK"));
        PledgeIntent normalized = withMissingFields(result.intent());
        String summary = summarize(normalized);
        String intentJson = json(normalized);
        repository.updateIntent(id, userId, summary, intentJson);
        String assistant = normalized.missingFields().isEmpty()
                ? summary + " 내용이 맞는지 확인해주세요."
                : summary + " 확인이 필요한 항목: " + String.join(", ", normalized.missingFields());
        repository.addMessage(id, "ASSISTANT", assistant, intentJson);
        return response(requireOwned(id, userId), assistant, result.source());
    }

    private PledgeIntent fallback(String message, String previousJson) {
        PledgeIntent previous = read(previousJson);
        String lower = message.toLowerCase(Locale.ROOT);
        String type = previous == null ? null : previous.pledgeType();
        if (lower.contains("고향") || lower.contains("답례품")) type = "HOMETOWN_DONATION";
        else if (lower.contains("유산") || lower.contains("유언")) type = "LEGACY_DONATION";
        else if (lower.contains("문화유산") || lower.contains("문화재")) type = "CULTURAL_HERITAGE_DONATION";
        else if (lower.contains("봉사")) type = "VOLUNTEER";
        else if (type == null && (lower.contains("기부") || lower.contains("후원"))) type = "DONATION";

        BigDecimal amount = previous == null ? null : previous.amount();
        Matcher amountMatcher = AMOUNT.matcher(message);
        if (amountMatcher.find()) {
            amount = new BigDecimal(amountMatcher.group(1).replace(",", ""));
            if ("만원".equals(amountMatcher.group(2))) amount = amount.multiply(BigDecimal.valueOf(10_000));
        }
        String frequency = previous == null ? null : previous.frequency();
        if (lower.contains("매월") || lower.contains("월정기") || lower.contains("정기")) frequency = "MONTHLY";
        else if (lower.contains("매년") || lower.contains("연간")) frequency = "ANNUAL";
        else if (lower.contains("일시") || lower.contains("한 번") || lower.contains("한번")) frequency = "ONE_TIME";
        if ("VOLUNTEER".equals(type)) frequency = "NOT_APPLICABLE";

        String region = previous == null ? null : previous.region();
        for (String candidate : List.of("부산", "서울", "대구", "광주", "대전", "울산", "인천", "제주")) {
            if (message.contains(candidate)) region = candidate;
        }
        String beneficiary = previous == null ? null : previous.beneficiary();
        if (lower.contains("아동")) beneficiary = (region == null ? "" : region + " ") + "지역 아동";
        else if (lower.contains("어르신") || lower.contains("노인")) beneficiary = (region == null ? "" : region + " ") + "지역 어르신";
        else if (lower.contains("유기견") || lower.contains("동물")) beneficiary = "유기동물 보호";
        else if (beneficiary == null && region != null) beneficiary = region + " 지역사회";

        String reward = previous == null ? null : previous.rewardPreference();
        if (lower.contains("답례품 필요 없") || lower.contains("답례품 안")) reward = "NONE";
        else if (lower.contains("답례품")) reward = "UNSPECIFIED";

        return new PledgeIntent(type, beneficiary, amount, frequency,
                previous == null ? LocalDate.now() : previous.startDate(), region, reward,
                previous == null ? null : previous.taxDeductionConsent(),
                previous == null ? null : previous.privacyConsent(), message, List.of());
    }

    private PledgeIntent withMissingFields(PledgeIntent intent) {
        if (intent == null) intent = new PledgeIntent(null, null, null, null, null, null, null, null, null, null, List.of());
        List<String> missing = new ArrayList<>();
        if (blank(intent.pledgeType())) missing.add("pledgeType");
        if (blank(intent.beneficiary())) missing.add("beneficiary");
        boolean monetary = !"VOLUNTEER".equals(intent.pledgeType());
        if (monetary && intent.amount() == null) missing.add("amount");
        if (monetary && blank(intent.frequency())) missing.add("frequency");
        if ("HOMETOWN_DONATION".equals(intent.pledgeType()) && blank(intent.region())) missing.add("region");
        return new PledgeIntent(intent.pledgeType(), intent.beneficiary(), intent.amount(), intent.frequency(),
                intent.startDate() == null ? LocalDate.now() : intent.startDate(), intent.region(),
                intent.rewardPreference(), intent.taxDeductionConsent(), intent.privacyConsent(),
                intent.specialConditions(), missing);
    }

    private String summarize(PledgeIntent intent) {
        String amount = intent.amount() == null ? "금액 미정" : intent.amount().stripTrailingZeros().toPlainString() + "원";
        String frequency = switch (string(intent.frequency())) {
            case "MONTHLY" -> "매월";
            case "ANNUAL" -> "매년";
            case "ONE_TIME" -> "일시";
            case "NOT_APPLICABLE" -> "봉사 참여";
            default -> "주기 미정";
        };
        return "%s에 %s %s 약정".formatted(string(intent.beneficiary()).isBlank() ? "수혜처 미정" : intent.beneficiary(), frequency, amount);
    }

    private ConsultationResponse response(Map<String, Object> row, String assistant, String source) {
        return new ConsultationResponse(((Number) row.get("id")).longValue(),
                string(row.get("consultation_status")), string(row.get("intent_summary")),
                read(string(row.get("extracted_preferences_json"))), assistant, source);
    }

    private Map<String, Object> requireOwned(Long id, Long userId) {
        return repository.findOwned(id, userId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "CONSULTATION_NOT_FOUND", "AI 약정 상담을 찾을 수 없습니다."));
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("약정 의사를 저장할 수 없습니다.", e); }
    }

    private PledgeIntent read(String json) {
        if (blank(json)) return null;
        try { return objectMapper.readValue(json, PledgeIntent.class); }
        catch (JsonProcessingException e) { return null; }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }
}
