package com.pixelcare.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 체결된 약정서 파일에서 내용을 되읽는 Upstage 문서 API 어댑터.
 *
 * Document Parse로 서명본의 글자를 뽑고, Information Extract로 약정 항목을 구조화한다.
 * 대화용 Solar 호출(UpstageApiClient)과는 쓰는 엔드포인트도 목적도 달라 따로 둔다.
 */
@Component
public class UpstageDocumentClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String baseUrl;

    public UpstageDocumentClient(
            @Value("${upstage.api.key:}") String apiKey,
            @Value("${upstage.api.base-url:https://api.upstage.ai/v1}") String baseUrl
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 두 호출 모두 문서 한 편을 통째로 읽는다. 대화 호출보다 오래 걸린다.
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restTemplate = new RestTemplate(factory);
    }

    /** 약정서에서 뽑아낸 항목. 읽어내지 못한 값은 null이다. */
    public record ExtractedPledge(
            String applicantName,
            String organizationName,
            String pledgeAmount,
            String pledgeFrequency,
            String startDate
    ) {}

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("your_upstage");
    }

    /**
     * Document Parse로 문서의 글자를 뽑는다.
     * 기본 응답은 html만 채워지므로 text 형식을 함께 요청한다.
     */
    public Optional<String> parseText(byte[] pdfBytes, String filename) {
        if (!isConfigured() || pdfBytes == null || pdfBytes.length == 0) return Optional.empty();
        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("document", new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            form.add("model", "document-parse");
            form.add("output_formats", "[\"text\"]");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/document-digitization",
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return Optional.empty();
            String text = objectMapper.readTree(response.getBody()).path("content").path("text").asText("");
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            System.err.println("Upstage Document Parse 실패: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Information Extract로 약정 항목을 고정 스키마에 맞춰 구조화한다. */
    public Optional<ExtractedPledge> extractPledge(byte[] pdfBytes) {
        if (!isConfigured() || pdfBytes == null || pdfBytes.length == 0) return Optional.empty();
        try {
            String dataUri = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);

            Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "applicant_name", field("약정자 성명"),
                            "organization_name", field("주관 또는 수혜기관 이름"),
                            "pledge_amount", field("약정 금액, 숫자만"),
                            "pledge_frequency", field("약정 주기"),
                            "start_date", field("약정 개시일자, YYYY-MM-DD")
                    )
            );
            Map<String, Object> body = Map.of(
                    "model", "information-extract",
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of(
                                    "type", "image_url",
                                    "image_url", Map.of("url", dataUri)
                            ))
                    )),
                    "response_format", Map.of(
                            "type", "json_schema",
                            "json_schema", Map.of("name", "pledge", "schema", schema)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/information-extraction",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return Optional.empty();

            JsonNode choices = objectMapper.readTree(response.getBody()).path("choices");
            if (!choices.isArray() || choices.isEmpty()) return Optional.empty();
            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) return Optional.empty();

            JsonNode parsed = objectMapper.readTree(content);
            return Optional.of(new ExtractedPledge(
                    nullable(parsed, "applicant_name"),
                    nullable(parsed, "organization_name"),
                    nullable(parsed, "pledge_amount"),
                    nullable(parsed, "pledge_frequency"),
                    nullable(parsed, "start_date")
            ));
        } catch (Exception e) {
            System.err.println("Upstage Information Extract 실패: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> field(String description) {
        return Map.of("type", "string", "description", description);
    }

    private String nullable(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? null : value;
    }
}
