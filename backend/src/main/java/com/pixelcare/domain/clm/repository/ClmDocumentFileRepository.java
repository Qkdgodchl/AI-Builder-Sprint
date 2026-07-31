package com.pixelcare.domain.clm.repository;

import com.pixelcare.domain.clm.entity.ClmDocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClmDocumentFileRepository extends JpaRepository<ClmDocumentFile, Long> {
    boolean existsByClmDocumentIdAndFileTypeAndIsDeletedFalse(Long clmDocumentId, String fileType);
    List<ClmDocumentFile> findByClmDocumentIdAndIsDeletedFalseOrderByIdAsc(Long clmDocumentId);
    Optional<ClmDocumentFile> findByIdAndIsDeletedFalse(Long id);
}
