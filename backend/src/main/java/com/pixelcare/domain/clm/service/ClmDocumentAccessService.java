package com.pixelcare.domain.clm.service;

import com.pixelcare.domain.clm.entity.ClmDocument;
import com.pixelcare.domain.clm.repository.ClmDocumentAccessRepository;
import com.pixelcare.domain.clm.repository.ClmDocumentRepository;
import com.pixelcare.global.auth.CurrentUser;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ClmDocumentAccessService {

    private final ClmDocumentRepository documentRepository;
    private final ClmDocumentAccessRepository accessRepository;

    public ClmDocumentAccessService(
            ClmDocumentRepository documentRepository,
            ClmDocumentAccessRepository accessRepository
    ) {
        this.documentRepository = documentRepository;
        this.accessRepository = accessRepository;
    }

    public ClmDocument requireAccess(Long documentId, CurrentUser user) {
        ClmDocument document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "CLM_DOCUMENT_NOT_FOUND",
                        "전자서명 서류를 찾을 수 없습니다."
                ));
        boolean operator = user.hasRole("OPERATOR") || user.hasRole("ROLE_OPERATOR");
        boolean owner = user.id().equals(document.getApplicantUserId());
        boolean centerManager = (user.hasRole("CENTER_MANAGER") || user.hasRole("ROLE_CENTER_MANAGER"))
                && accessRepository.managerCanAccess(user.id(), documentId);
        if (!operator && !owner && !centerManager) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "CLM_DOCUMENT_FORBIDDEN",
                    "해당 전자서명 서류를 볼 권한이 없습니다."
            );
        }
        return document;
    }
}
