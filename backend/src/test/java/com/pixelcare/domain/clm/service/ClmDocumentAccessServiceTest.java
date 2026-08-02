package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmDocumentAccessRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class ClmDocumentAccessServiceTest {

    @Mock ClmDocumentRepository documentRepository;
    @Mock ClmDocumentAccessRepository accessRepository;
    private ClmDocumentAccessService service;
    private ClmDocument document;

    @BeforeEach
    void setUp() {
        service = new ClmDocumentAccessService(documentRepository, accessRepository);
        document = new ClmDocument(
                8L, "봉사 모집글", 11L, "신청자", "user@example.com", null,
                "modusign-1", "participant-1", "template-1",
                "https://sign.example", LocalDateTime.now().plusMinutes(10)
        );
        setField(document, "id", 33L);
        when(documentRepository.findByIdAndIsDeletedFalse(33L)).thenReturn(Optional.of(document));
    }

    @Test
    void managerCanOpenOnlyDocumentsBelongingToManagedOrganization() {
        CurrentUser manager = new CurrentUser(21L, "manager@example.com", "센터장",
                Set.of("CENTER_MANAGER"));
        when(accessRepository.managerCanAccess(21L, 33L)).thenReturn(true);

        assertThat(service.requireAccess(33L, manager)).isSameAs(document);
    }

    @Test
    void managerFromAnotherOrganizationIsForbidden() {
        CurrentUser manager = new CurrentUser(22L, "other@example.com", "다른센터",
                Set.of("CENTER_MANAGER"));
        when(accessRepository.managerCanAccess(22L, 33L)).thenReturn(false);

        assertThatThrownBy(() -> service.requireAccess(33L, manager))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CLM_DOCUMENT_FORBIDDEN"));
    }
}
