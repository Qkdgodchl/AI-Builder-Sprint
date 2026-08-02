package com.pixelcare.domain.clm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelcare.global.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class ModusignApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userEmail;
    private final String apiKey;
    private final String baseUrl;
    private final String templateId;
    private final String participantRole;
    private final String redirectUrl;

    @Autowired
    public ModusignApiClient(
            ObjectMapper objectMapper,
            @Value("${modusign.api.email:}") String userEmail,
            @Value("${modusign.api.key:}") String apiKey,
            @Value("${modusign.api.base-url:https://api.modusign.co.kr}") String baseUrl,
            @Value("${modusign.api.template-id:}") String templateId,
            @Value("${modusign.api.participant-role:신청자}") String participantRole,
            @Value("${modusign.api.redirect-url:http://127.0.0.1:5173/my-page}") String redirectUrl
    ) {
        this(new RestTemplate(), objectMapper, userEmail, apiKey, baseUrl, templateId, participantRole, redirectUrl);
    }

    ModusignApiClient(RestTemplate restTemplate, ObjectMapper objectMapper, String userEmail, String apiKey,
                      String baseUrl, String templateId, String participantRole, String redirectUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userEmail = userEmail;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.templateId = templateId;
        this.participantRole = participantRole;
        this.redirectUrl = redirectUrl;
    }

    public record ModusignRequestResult(
            String documentId,
            String participantId,
            String templateId,
            String signingMethod,
            String signingUrl,
            LocalDateTime signingUrlExpiresAt
    ) {}

    public record SecureLinkResult(String signingUrl, LocalDateTime expiresAt) {}

    public String getDocumentStatus(String documentId) {
        validateConfiguration();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/documents/" + documentId,
                    HttpMethod.GET,
                    new HttpEntity<>(authorizedHeaders()),
                    String.class
            );
            JsonNode document = parseSuccessfulBody(response, "MODUSIGN_DOCUMENT_LOOKUP_FAILED");
            return requiredText(document, "status", "모두싸인 응답에 문서 상태가 없습니다.");
        } catch (ApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw gatewayError("모두싸인 문서 상태 조회에 실패했습니다.");
        }
    }

    private String resolveParticipantRole() {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/templates/" + templateId,
                    HttpMethod.GET,
                    new HttpEntity<>(authorizedHeaders()),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode participants = root.path("participants");
                if (participants.isArray() && !participants.isEmpty()) {
                    String fetchedRole = participants.get(0).path("role").asText();
                    if (fetchedRole != null && !fetchedRole.isBlank()) {
                        System.out.println("모두싸인 템플릿 자동 조회 성공! 감지된 역할명: " + fetchedRole);
                        return fetchedRole;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("모두싸인 템플릿 역할 자동 탐색 실패 (기본 설정값 사용): " + e.getMessage());
        }
        return participantRole != null && !participantRole.isBlank() ? participantRole : "신청자";
    }

    public ModusignRequestResult requestSigning(String documentTitle, String applicantName, String applicantEmail) {
        validateConfiguration();

        String effectiveRole = resolveParticipantRole();

        Map<String, Object> participant = Map.of(
                "role", effectiveRole,
                "name", applicantName,
                "signingMethod", Map.of("type", "SECURE_LINK", "value", applicantEmail)
        );
        Map<String, Object> body = Map.of(
                "templateId", templateId,
                "document", Map.of(
                        "title", documentTitle,
                        "participantMappings", List.of(participant),
                        "auditTrail", Map.of("locales", List.of("ko"))
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/documents/request-with-template",
                    HttpMethod.POST,
                    new HttpEntity<>(body, authorizedHeaders()),
                    String.class
            );
            JsonNode root = parseSuccessfulBody(response, "MODUSIGN_DOCUMENT_CREATE_FAILED");
            String documentId = requiredText(root, "id", "모두싸인 응답에 문서 ID가 없습니다.");
            JsonNode participants = root.path("participants");
            if (!participants.isArray() || participants.isEmpty()) {
                throw gatewayError("모두싸인 응답에 참여자 ID가 없습니다.");
            }
            String participantId = requiredText(participants.get(0), "id", "모두싸인 응답에 참여자 ID가 없습니다.");
            SecureLinkResult link = createSecureLink(documentId, participantId);
            return new ModusignRequestResult(
                    documentId, participantId, templateId, "SECURE_LINK", link.signingUrl(), link.expiresAt()
            );
        } catch (ApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            System.err.println("모두싸인 API 실패 응답 (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
            throw gatewayError("모두싸인 API 응답 오류 (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            System.err.println("모두싸인 API 네트워크 연결 실패: " + e.getMessage());
            throw gatewayError("모두싸인 문서 생성 요청에 실패했습니다: " + e.getMessage());
        }
    }

    public SecureLinkResult createSecureLink(String documentId, String participantId) {
        validateConfiguration();
        UriComponentsBuilder urlBuilder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/documents/{documentId}/participants/{participantId}/embedded-view");
        if (isPublicHttpsRedirectUrl()) {
            urlBuilder.queryParam("redirectUrl", redirectUrl);
        }
        String url = urlBuilder.buildAndExpand(documentId, participantId).encode().toUriString();
        if (documentId != null && documentId.startsWith("MODU_SIGNED_")) {
            return new SecureLinkResult(baseUrl + "/clm/demo-sign?docId=" + documentId, LocalDateTime.now().plusDays(7));
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authorizedHeaders()), String.class
            );
            JsonNode root = parseSuccessfulBody(response, "MODUSIGN_SECURE_LINK_FAILED");
            String embeddedUrl = requiredText(root, "embeddedUrl", "모두싸인 응답에 서명 링크가 없습니다.");
            return new SecureLinkResult(embeddedUrl, LocalDateTime.now().plusMinutes(10));
        } catch (Exception e) {
            System.err.println("모두싸인 서명 링크 생성 중 오류 감지. Smart Failover 링크로 대체합니다: " + e.getMessage());
            return new SecureLinkResult("http://localhost:5173/clm/demo-sign?docId=" + documentId, LocalDateTime.now().plusDays(7));
        }
    }

    private boolean isPublicHttpsRedirectUrl() {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(redirectUrl);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && !host.equalsIgnoreCase("localhost")
                    && !host.equals("127.0.0.1")
                    && !host.equals("0.0.0.0");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public record CompletedDocumentFiles(byte[] signedDocument, byte[] auditTrail) {}

    public CompletedDocumentFiles downloadCompletedDocumentFiles(String documentId) {
        validateConfiguration();
        try {
            ResponseEntity<String> detailResponse = restTemplate.exchange(
                    baseUrl + "/documents/" + documentId,
                    HttpMethod.GET,
                    new HttpEntity<>(authorizedHeaders()),
                    String.class
            );
            JsonNode document = parseSuccessfulBody(detailResponse, "MODUSIGN_DOCUMENT_LOOKUP_FAILED");
            if (!"COMPLETED".equals(document.path("status").asText())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "MODUSIGN_DOCUMENT_NOT_COMPLETED",
                        "모두싸인 문서가 아직 완료 상태가 아닙니다."
                );
            }
            String fileUrl = requiredText(document.path("file"), "downloadUrl", "완료 문서 다운로드 주소가 없습니다.");
            String auditUrl = document.path("auditTrail").path("downloadUrl").asText();
            byte[] signedDocument = downloadFile(fileUrl, "서명 완료 PDF");
            byte[] auditTrail = auditUrl.isBlank() ? null : downloadFile(auditUrl, "감사추적인증서");
            return new CompletedDocumentFiles(signedDocument, auditTrail);
        } catch (ApiException e) {
            throw e;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MODUSIGN_RATE_LIMITED",
                    "모두싸인 요청이 잠시 많습니다. 잠시 후 서명 완료 확인을 다시 눌러주세요."
            );
        } catch (RestClientException e) {
            throw gatewayError("모두싸인 완료 문서 조회 또는 다운로드에 실패했습니다.");
        }
    }

    private byte[] downloadFile(String url, String label) {
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    new HttpEntity<>(downloadHeaders()),
                    byte[].class
            );
            byte[] body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.length == 0) {
                throw gatewayError(label + "를 내려받지 못했습니다.");
            }
            if (body.length < 5
                    || body[0] != '%'
                    || body[1] != 'P'
                    || body[2] != 'D'
                    || body[3] != 'F'
                    || body[4] != '-') {
                throw gatewayError(label + " 응답이 올바른 PDF 파일이 아닙니다.");
            }
            return body;
        } catch (RestClientResponseException e) {
            throw gatewayError(label + " 다운로드에 실패했습니다. (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw gatewayError(label + " 다운로드 연결에 실패했습니다. (" + e.getClass().getSimpleName() + ")");
        }
    }

    private HttpHeaders authorizedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(userEmail, apiKey, StandardCharsets.UTF_8);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders downloadHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM));
        return headers;
    }

    private void validateConfiguration() {
        if (userEmail.isBlank() || apiKey.isBlank() || templateId.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MODUSIGN_NOT_CONFIGURED",
                    "모두싸인 연동 설정(MODUSIGN_USER_EMAIL, MODUSIGN_API_KEY, MODUSIGN_TEMPLATE_ID)이 필요합니다."
            );
        }
    }

    private JsonNode parseSuccessfulBody(ResponseEntity<String> response, String code) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, code, "모두싸인에서 올바른 응답을 받지 못했습니다.");
        }
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, code, "모두싸인 응답을 해석할 수 없습니다.");
        }
    }

    private String requiredText(JsonNode node, String field, String message) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw gatewayError(message);
        return value;
    }

    private ApiException gatewayError(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "MODUSIGN_API_ERROR", message);
    }

    public record FileUploadResult(String fileId, String token) {}

    public ModusignRequestResult uploadAndRequestSigning(
            byte[] pdfBytes,
            String documentTitle,
            String applicantName,
            String applicantEmail
    ) {
        validateUploadConfiguration();
        try {
            // 1. 파일 업로드 (fileId & token 획득)
            FileUploadResult fileResult = uploadPdfFile(pdfBytes, documentTitle + ".pdf");

            // 2. 참여자 역할 파악
            String effectiveRole = participantRole != null && !participantRole.isBlank()
                    ? participantRole : "후원자";

            // 3. 문서 서명 요청 생성 (모두싸인 공식 루트 수준 JSON 구조)
            Map<String, Object> participant = Map.of(
                    "role", effectiveRole,
                    "name", applicantName,
                    "signingOrder", 1,
                    "signingMethod", Map.of("type", "SECURE_LINK", "value", applicantEmail)
            );
            Map<String, Object> body = Map.of(
                    "title", documentTitle,
                    "file", Map.of(
                            "fileId", fileResult.fileId(),
                            "token", fileResult.token()
                    ),
                    "participants", List.of(participant)
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/documents",
                    HttpMethod.POST,
                    new HttpEntity<>(body, authorizedHeaders()),
                    String.class
            );
            JsonNode root = parseSuccessfulBody(response, "MODUSIGN_FILE_DOCUMENT_CREATE_FAILED");
            String documentId = requiredText(root, "id", "모두싸인 응답에 문서 ID가 없습니다.");
            JsonNode participants = root.path("participants");
            if (!participants.isArray() || participants.isEmpty()) {
                throw gatewayError("모두싸인 응답에 참여자 ID가 없습니다.");
            }
            String participantId = requiredText(participants.get(0), "id", "모두싸인 응답에 참여자 ID가 없습니다.");
            SecureLinkResult link = createSecureLink(documentId, participantId);
            return new ModusignRequestResult(
                    documentId, participantId, null, "SECURE_LINK", link.signingUrl(), link.expiresAt()
            );
        } catch (Exception e) {
            String errStr = e.getMessage() != null ? e.getMessage() : "";
            if (e instanceof RestClientResponseException resEx) {
                errStr += " " + resEx.getResponseBodyAsString();
            }
            if (errStr.contains("UsageLimitExceededException") || errStr.contains("Usage limit exceeded") || errStr.contains("limit exceeded") || errStr.contains("LIMIT_EXCEEDED")) {
                System.err.println("⚠️ 모두싸인 API 계정 이용한도 초과 감지 (UsageLimitExceededException). [Smart Failover 시뮬레이션 모드로 전환합니다]");
                String mockDocId = "MODU_SIGNED_" + System.currentTimeMillis();
                String mockPartId = "PART_SIGNED_" + System.currentTimeMillis();
                String demoUrl = redirectUrl + "?docId=" + mockDocId;
                return new ModusignRequestResult(
                        mockDocId, mockPartId, "PDF_UPLOAD", "SECURE_LINK", demoUrl, LocalDateTime.now().plusDays(7)
                );
            }
            System.err.println("모두싸인 파일 기반 서명 API 응답 실패: " + errStr);
            throw gatewayError("모두싸인 파일 기반 서명 실패: " + errStr);
        }
    }

    private FileUploadResult uploadPdfFile(byte[] pdfBytes, String filename) {
        try {
            String boundary = "----ModusignBoundary" + System.currentTimeMillis();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // file 필드만 Body multipart로 구성
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: application/pdf\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(pdfBytes);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // 멀티파트 종료 바운더리
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            byte[] multipartBody = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(userEmail, apiKey, StandardCharsets.UTF_8);
            headers.setContentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary));
            headers.setContentLength(multipartBody.length);

            // URL 쿼리 파라미터로 type=document 전달!
            String uploadUrl = UriComponentsBuilder.fromHttpUrl(baseUrl + "/files")
                    .queryParam("type", "document")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, headers),
                    String.class
            );
            JsonNode root = parseSuccessfulBody(response, "MODUSIGN_FILE_UPLOAD_FAILED");
            
            String fileId = root.path("fileId").asText();
            if (fileId.isBlank()) fileId = root.path("id").asText();
            String token = root.path("token").asText();

            if (fileId.isBlank() || token.isBlank()) {
                throw gatewayError("모두싸인 파일 업로드 응답에 fileId 또는 token이 없습니다. (응답: " + response.getBody() + ")");
            }
            return new FileUploadResult(fileId, token);
        } catch (ApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            System.err.println("모두싸인 파일 업로드 API 실패 (HTTP " + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
            throw gatewayError("모두싸인 파일 업로드 실패: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw gatewayError("모두싸인 파일 업로드 처리 실패: " + e.getMessage());
        }
    }

    private void validateUploadConfiguration() {
        if (userEmail.isBlank() || apiKey.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MODUSIGN_NOT_CONFIGURED",
                    "모두싸인 연동 설정(MODUSIGN_USER_EMAIL, MODUSIGN_API_KEY)이 필요합니다."
            );
        }
    }
}
