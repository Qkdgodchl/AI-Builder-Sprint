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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiConsultationService {

    private static final Pattern AMOUNT = Pattern.compile("(\\d[\\d,]*)\\s*(만원|원)");
    private static final Set<String> PLEDGE_TYPES = Set.of(
            "DONATION", "HOMETOWN_DONATION", "VOLUNTEER", "LEGACY_DONATION", "CULTURAL_HERITAGE_DONATION");
    private static final Set<String> FREQUENCIES = Set.of(
            "ONE_TIME", "WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "BIANNUAL", "ANNUAL", "FLEXIBLE", "NOT_APPLICABLE");
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
    public ConsultationResponse start(Long userId, String message, boolean externalAiConsent) {
        Long id = repository.create(userId);
        repository.addMessage(id, "USER", message, null);
        return structureAndSave(id, userId, message, null, externalAiConsent);
    }

    @Transactional
    public ConsultationResponse addMessage(Long id, Long userId, String message, boolean externalAiConsent) {
        Map<String, Object> row = requireOwned(id, userId);
        repository.addMessage(id, "USER", message, null);
        return structureAndSave(id, userId, message, string(row.get("extracted_preferences_json")), externalAiConsent);
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

    private ConsultationResponse structureAndSave(Long id, Long userId, String message, String previousJson,
                                                   boolean externalAiConsent) {
        if (externalAiConsent) repository.recordExternalAiConsent(id, userId);
        UpstageApiClient.StructuredIntent result = (externalAiConsent
                ? upstageApiClient.structurePledgeIntent(message, previousJson)
                : Optional.<UpstageApiClient.StructuredIntent>empty())
                .orElseGet(() -> new UpstageApiClient.StructuredIntent(fallback(message, previousJson), "RULE_FALLBACK"));
        PledgeIntent normalized = withMissingFields(result.intent());
        String summary = summarize(normalized);
        String intentJson = json(normalized);
        repository.updateIntent(id, userId, summary, intentJson);
        String assistant = buildProfessionalAssistantMessage(normalized, message);
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
        if (lower.contains("매주") || lower.contains("주간")) frequency = "WEEKLY";
        else if (lower.contains("격주") || lower.contains("2주")) frequency = "BIWEEKLY";
        else if (lower.contains("매월") || lower.contains("월정기") || lower.contains("월간")) frequency = "MONTHLY";
        else if (lower.contains("분기")) frequency = "QUARTERLY";
        else if (lower.contains("반기") || lower.contains("6개월")) frequency = "BIANNUAL";
        else if (lower.contains("매년") || lower.contains("연간")) frequency = "ANNUAL";
        else if (lower.contains("수시") || lower.contains("자율")) frequency = "FLEXIBLE";
        else if (lower.contains("일시") || lower.contains("한 번") || lower.contains("한번") || lower.contains("1회")) frequency = "ONE_TIME";
        else if (frequency == null && (lower.contains("정기") || lower.contains("후원"))) frequency = "MONTHLY";
        if ("VOLUNTEER".equals(type)) frequency = "NOT_APPLICABLE";

        String region = previous == null ? null : previous.region();
        if (message.contains("지역:")) {
            int start = message.indexOf("지역:") + 3;
            int end = message.indexOf(",", start);
            if (end < 0) end = message.indexOf("]", start);
            if (end > start) region = message.substring(start, end).trim();
        }
        if (region == null || region.isBlank()) {
            for (String candidate : List.of("부산 영도구", "부산 해운대구", "부산 금정구", "부산 수영구", "부산 남구", "부산 동래구", "부산 진구", "부산", "서울", "대구", "광주", "대전", "울산", "인천", "제주")) {
                if (message.contains(candidate)) {
                    region = candidate;
                    break;
                }
            }
        }
        if (region == null || region.isBlank()) region = "부산광역시 영도구";

        String beneficiary = previous == null ? null : previous.beneficiary();
        if (message.contains("주관기관:")) {
            int start = message.indexOf("주관기관:") + 5;
            int end = message.indexOf("]", start);
            if (end > start) beneficiary = message.substring(start, end).trim();
        }
        if (beneficiary == null || beneficiary.isBlank()) {
            if (message.contains("프로그램:")) {
                int start = message.indexOf("프로그램:") + 5;
                int end = message.indexOf(",", start);
                if (end > start) beneficiary = message.substring(start, end).trim();
            }
        }
        if (beneficiary == null || beneficiary.isBlank()) {
            if (lower.contains("아동")) beneficiary = region + " 지역 아동";
            else if (lower.contains("어르신") || lower.contains("노인")) beneficiary = region + " 지역 어르신";
            else if (lower.contains("유기견") || lower.contains("동물")) beneficiary = "유기동물 보호";
            else beneficiary = region + " 지역사회";
        }

        String reward = previous == null ? null : previous.rewardPreference();
        String giftItem = previous == null ? null : previous.giftItem();

        if (lower.contains("답례품 안") || lower.contains("답례품 필요 없") || lower.contains("미수령") || lower.contains("기탁")) {
            reward = "NONE";
            giftItem = "답례품 미수령 (전액 기탁)";
        } else if (lower.contains("미역") || lower.contains("다시마") || lower.contains("기장")) {
            reward = "UNSPECIFIED";
            giftItem = "부산 기장 명품 미역·다시마 세트";
        } else if (lower.contains("육포") || lower.contains("한우")) {
            reward = "UNSPECIFIED";
            giftItem = "부산 명품 한우 수제 육포 세트";
        } else if (lower.contains("고구마") || lower.contains("어묵") || lower.contains("앙금")) {
            reward = "UNSPECIFIED";
            giftItem = "부산 영도 고구마 앙금빵 & 어묵";
        } else if (lower.contains("커피") || lower.contains("원두") || lower.contains("모모스")) {
            reward = "UNSPECIFIED";
            giftItem = "부산 영도 모모스 스페셜티 원두";
        } else if (lower.contains("동백전") || lower.contains("지역화폐") || giftItem == null) {
            if ("HOMETOWN_DONATION".equals(type)) {
                reward = "UNSPECIFIED";
                giftItem = "부산 동백전 지역화폐 30% 포인트";
            }
        }

        String localGovCode = "HOMETOWN_DONATION".equals(type) ? "26000" : (previous == null ? null : previous.localGovCode());
        BigDecimal taxCreditAmount = amount == null ? null : (amount.compareTo(new BigDecimal("100000")) <= 0 ? amount : new BigDecimal("100000"));
        String heritageTarget = previous == null ? null : previous.heritageTarget();
        String bequestType = previous == null ? null : previous.bequestType();

        return new PledgeIntent(type, beneficiary, amount, frequency,
                previous == null ? LocalDate.now() : previous.startDate(), region, reward,
                previous == null ? null : previous.taxDeductionConsent(),
                previous == null ? null : previous.privacyConsent(), message,
                giftItem, localGovCode, taxCreditAmount, heritageTarget, bequestType, List.of());
    }

    private PledgeIntent withMissingFields(PledgeIntent intent) {
        if (intent == null) intent = new PledgeIntent(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
        String pledgeType = intent.pledgeType() != null && PLEDGE_TYPES.contains(intent.pledgeType())
                ? intent.pledgeType() : null;
        String frequency = intent.frequency() != null && FREQUENCIES.contains(intent.frequency())
                ? intent.frequency() : null;
        if ("VOLUNTEER".equals(pledgeType)) frequency = "NOT_APPLICABLE";
        List<String> missing = new ArrayList<>();
        if (blank(pledgeType)) missing.add("pledgeType");
        if (blank(intent.beneficiary())) missing.add("beneficiary");
        boolean monetary = !"VOLUNTEER".equals(pledgeType);
        BigDecimal amount = intent.amount() != null && intent.amount().signum() > 0 ? intent.amount() : null;
        if (monetary && amount == null) missing.add("amount");
        if (monetary && blank(frequency)) missing.add("frequency");
        if ("HOMETOWN_DONATION".equals(pledgeType) && blank(intent.region())) missing.add("region");
        return new PledgeIntent(pledgeType, intent.beneficiary(), amount, frequency,
                intent.startDate() == null ? LocalDate.now() : intent.startDate(), intent.region(),
                intent.rewardPreference(), intent.taxDeductionConsent(), intent.privacyConsent(),
                intent.specialConditions(), intent.giftItem(), intent.localGovCode(), intent.taxCreditAmount(),
                intent.heritageTarget(), intent.bequestType(), missing);
    }

    private String resolveFrequencyKorean(String freq) {
        if (freq == null) return "일시 기부";
        return switch (freq) {
            case "ONE_TIME" -> "일시 기부";
            case "WEEKLY" -> "매주 정기 후원";
            case "BIWEEKLY" -> "격주 정기 후원";
            case "MONTHLY" -> "매월 정기 후원";
            case "QUARTERLY" -> "분기별 정기 후원 (3개월)";
            case "BIANNUAL" -> "반기별 정기 후원 (6개월)";
            case "ANNUAL" -> "연간 정기 후원";
            case "FLEXIBLE" -> "수시 / 자율 후원";
            case "NOT_APPLICABLE" -> "해당 없음 (자원봉사)";
            default -> freq;
        };
    }

    private String summarize(PledgeIntent intent) {
        String amount = intent.amount() == null ? "금액 미정" : intent.amount().stripTrailingZeros().toPlainString() + "원";
        String frequency = switch (string(intent.frequency())) {
            case "WEEKLY" -> "매주";
            case "BIWEEKLY" -> "격주";
            case "MONTHLY" -> "매월";
            case "QUARTERLY" -> "분기별";
            case "BIANNUAL" -> "반기별";
            case "ANNUAL" -> "매년";
            case "ONE_TIME" -> "일시";
            case "FLEXIBLE" -> "수시";
            case "NOT_APPLICABLE" -> "봉사 참여";
            default -> "주기 미정";
        };
        return "%s에 %s %s 약정".formatted(string(intent.beneficiary()).isBlank() ? "수혜처 미정" : intent.beneficiary(), frequency, amount);
    }

    private String buildProfessionalAssistantMessage(PledgeIntent intent, String userMessage) {
        StringBuilder sb = new StringBuilder();
        String type = intent.pledgeType() != null ? intent.pledgeType() : "DONATION";
        String beneficiary = (intent.beneficiary() != null && !intent.beneficiary().isBlank())
                ? intent.beneficiary() : "픽셀케어 지정 후원처";
        List<String> missing = intent.missingFields() != null ? intent.missingFields() : List.of();

        // 1. 지역이나 수혜대상이 미정일 경우
        if (intent.region() == null || intent.region().isBlank()) {
            return "기부 및 선행에 뜻을 모아주셔서 진심으로 감사드려요! 💝\n\n혹시 후원하거나 활동하고 싶으신 **특별한 지역**(예: 부산광역시, 서울, 해운대구, 전국 등)이 있으신가요? 🏡";
        }

        if (intent.beneficiary() == null || intent.beneficiary().isBlank()) {
            return "%s 지역을 향한 따뜻한 마음에 감사드립니다! 🌟\n\n혹시 응원하고 싶으신 **수혜 대상이나 분야**(예: 지역 아동, 어르신, 유기동물 보호, 문화재 보존 등)가 있으신가요? 🤝".formatted(intent.region());
        }

        // 2. 주기가 빠진 경우
        if (missing.contains("frequency")) {
            if ("VOLUNTEER".equals(type)) {
                return "%s의 %s 봉사 활동에 관심을 가져주셔서 감사해요! 🙌\n\n혹시 어떤 주기로 참여하길 원하시나요? (예: 주말 매주, 격주, 하루 일시 참여 등)".formatted(intent.region(), beneficiary);
            }
            return "%s %s 후원에 감사드려요! 💝\n\n기부 납부 주기는 어떻게 생각하고 계신가요?\n(예: 일시 기부, 매월 정기 후원, 분기별 후원, 매년 후원 등)".formatted(intent.region(), beneficiary);
        }

        // 3. 금액이 빠진 경우
        if (missing.contains("amount") && !"VOLUNTEER".equals(type)) {
            String freqKorean = resolveFrequencyKorean(intent.frequency());
            return "네, %s 약정으로 등록해 드릴게요! 📅\n\n혹시 생각하시는 기부 금액은 얼마 정도인가요?\n(예: 1만원, 3만원, 5만원, 10만원 등 편하게 말씀해 주세요!)".formatted(freqKorean);
        }

        // 4. 모든 필수 정보가 채워진 최종 상태
        sb.append("감사합니다! 약정 필수 정보가 모두 수집되었어요! ✨\n\n");
        sb.append("📋 [정리된 약정 내역]\n");
        sb.append(" • 후원 / 활동 지역: %s\n".formatted(intent.region()));
        sb.append(" • 수혜 대상 및 기관: %s\n".formatted(beneficiary));
        if (!"VOLUNTEER".equals(type) && intent.amount() != null) {
            sb.append(" • 약정 금액: %,d원 (%s)\n".formatted(intent.amount().longValue(), resolveFrequencyKorean(intent.frequency())));
        }
        if (intent.startDate() != null) {
            sb.append(" • 약정 개시일: %s\n".formatted(intent.startDate()));
        }

        if ("HOMETOWN_DONATION".equals(type)) {
            long val = intent.amount() != null ? intent.amount().longValue() : 0;
            long pts = (long)(val * 0.3);
            String selectedGift = intent.giftItem() != null ? intent.giftItem() : "부산 동백전 지역화폐 30% 포인트";
            sb.append("\n🎁 [고향사랑e음 답례품몰 현황 (ilovegohyang.go.kr)]\n");
            sb.append(" • 기부 답례품 포인트: %,d P (기부금 30%% 자동 산정)\n".formatted(pts));
            sb.append(" • 선택한 답례품: %s\n".formatted(selectedGift));
            sb.append(" • 세액공제 혜택: 10만원 이하 100%% 전액 환급 대상\n");
        } else if ("LEGACY_DONATION".equals(type) || "CULTURAL_HERITAGE_DONATION".equals(type)) {
            sb.append("\n🏛️ [유산/문화유산 보존]: 민법 제1060조 유증 기부 및 영구 보존 기금으로 지정 관리됩니다.\n");
        }

        sb.append("\n아래 카드 및 답례품몰에서 개시일자, 답례품, 세액공제 신청 및 희망 메시지를 최종 확인하신 뒤 **약정 의사 확정하기** 버튼을 눌러주세요! 😊");
        return sb.toString();
    }

    private ConsultationResponse response(Map<String, Object> row, String assistant, String source) {
        PledgeIntent intent = read(string(row.get("extracted_preferences_json")));
        String message = (assistant != null && !assistant.isBlank()) ? assistant : (intent != null ? buildProfessionalAssistantMessage(intent, "") : string(row.get("intent_summary")));
        return new ConsultationResponse(((Number) row.get("id")).longValue(),
                string(row.get("consultation_status")), string(row.get("intent_summary")),
                intent, message, source);
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
