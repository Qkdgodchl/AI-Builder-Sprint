package com.pixelcare.domain.clm.repository;

import com.pixelcare.domain.clm.entity.ClmDocumentVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClmDocumentVerificationRepository extends JpaRepository<ClmDocumentVerification, Long> {
    Optional<ClmDocumentVerification> findFirstByClmDocumentIdAndIsDeletedFalseOrderByIdDesc(Long clmDocumentId);
}
