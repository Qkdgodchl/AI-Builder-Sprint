package com.pixelcare.domain.clm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ModusignApiClientTest {

    @Test
    void readsCurrentDocumentStatus() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.modusign.test/documents/document-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {"id":"document-1","status":"COMPLETED"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ModusignApiClient client = new ModusignApiClient(
                restTemplate, new ObjectMapper(), "owner@example.com", "secret-key",
                "https://api.modusign.test", "template-1", "신청자", "http://localhost"
        );

        assertThat(client.getDocumentStatus("document-1")).isEqualTo("COMPLETED");
        server.verify();
    }

    @Test
    void createsTemplateDocumentWithSecureLinkAndUsesEmailApiKeyBasicAuth() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String expectedBasic = "Basic " + Base64.getEncoder()
                .encodeToString("owner@example.com:secret-key".getBytes(StandardCharsets.UTF_8));

        server.expect(requestTo("https://api.modusign.test/templates/template-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", expectedBasic))
                .andRespond(withSuccess(
                        """
                        {"participants":[{"role":"신청자"}]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        server.expect(requestTo("https://api.modusign.test/documents/request-with-template"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", expectedBasic))
                .andExpect(jsonPath("$.templateId").value("template-1"))
                .andExpect(jsonPath("$.document.participantMappings[0].role").value("신청자"))
                .andExpect(jsonPath("$.document.participantMappings[0].signingMethod.type").value("SECURE_LINK"))
                .andExpect(jsonPath("$.document.participantMappings[0].signingMethod.value").value("user@example.com"))
                .andExpect(jsonPath("$.document.auditTrail.locales[0]").value("ko"))
                .andRespond(withSuccess(
                        """
                        {"id":"document-1","participants":[{"id":"participant-1","name":"홍길동"}]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        server.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "/documents/document-1/participants/participant-1/embedded-view"
                )))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getURI().getQuery()).isNull())
                .andExpect(header("Authorization", expectedBasic))
                .andRespond(withSuccess(
                        """
                        {"embeddedUrl":"https://sign.modusign.test/secure-link"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ModusignApiClient client = new ModusignApiClient(
                restTemplate,
                new ObjectMapper(),
                "owner@example.com",
                "secret-key",
                "https://api.modusign.test",
                "template-1",
                "신청자",
                "http://127.0.0.1:5173/my-page"
        );

        ModusignApiClient.ModusignRequestResult result =
                client.requestSigning("봉사 참여 약정서", "홍길동", "user@example.com");

        assertThat(result.documentId()).isEqualTo("document-1");
        assertThat(result.participantId()).isEqualTo("participant-1");
        assertThat(result.signingMethod()).isEqualTo("SECURE_LINK");
        assertThat(result.signingUrl()).isEqualTo("https://sign.modusign.test/secure-link");
        server.verify();
    }

    @Test
    void downloadsCompletedPdfAndAuditTrail() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://api.modusign.test/documents/document-1"))
                .andRespond(withSuccess(
                        """
                        {
                          "status":"COMPLETED",
                          "file":{"downloadUrl":"https://download.test/signed.pdf"},
                          "auditTrail":{"downloadUrl":"https://download.test/audit.pdf"}
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));
        byte[] signedPdf = "%PDF-1.4 signed-pdf".getBytes(StandardCharsets.UTF_8);
        byte[] auditPdf = "%PDF-1.4 audit-pdf".getBytes(StandardCharsets.UTF_8);
        server.expect(requestTo("https://download.test/signed.pdf"))
                .andExpect(header("Accept", "application/pdf, application/octet-stream"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(signedPdf, MediaType.APPLICATION_PDF));
        server.expect(requestTo("https://download.test/audit.pdf"))
                .andExpect(header("Accept", "application/pdf, application/octet-stream"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(auditPdf, MediaType.APPLICATION_PDF));

        ModusignApiClient client = new ModusignApiClient(
                restTemplate, new ObjectMapper(), "owner@example.com", "secret-key",
                "https://api.modusign.test", "template-1", "신청자", "http://localhost"
        );
        ModusignApiClient.CompletedDocumentFiles files =
                client.downloadCompletedDocumentFiles("document-1");

        assertThat(files.signedDocument()).isEqualTo(signedPdf);
        assertThat(files.auditTrail()).isEqualTo(auditPdf);
        server.verify();
    }
}
