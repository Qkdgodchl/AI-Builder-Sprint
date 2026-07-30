package com.pixelcare.domain.clm.repository;

import com.pixelcare.domain.clm.entity.ClmDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClmDocumentRepository extends JpaRepository<ClmDocument, Long> {
    List<ClmDocument> findByApplicantEmailAndIsDeletedFalseOrderByIdDesc(String applicantEmail);
    List<ClmDocument> findByVolunteerIdAndIsDeletedFalseOrderByIdDesc(Long volunteerId);
    Optional<ClmDocument> findByIdAndIsDeletedFalse(Long id);
    Optional<ClmDocument> findByModusignDocumentIdAndIsDeletedFalse(String modusignDocumentId);
}
