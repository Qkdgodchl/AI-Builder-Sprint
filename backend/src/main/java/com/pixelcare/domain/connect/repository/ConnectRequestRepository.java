package com.pixelcare.domain.connect.repository;

import com.pixelcare.domain.connect.entity.ConnectRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectRequestRepository extends JpaRepository<ConnectRequest, Long> {

    Optional<ConnectRequest> findByPublicIdAndIsDeletedFalse(String publicId);

    // 아직 안 맡은 요청을 먼저, 그중에서도 원하는 사람이 많은 순으로 올린다.
    List<ConnectRequest> findByIsDeletedFalseOrderBySupportCountDescIdDesc();

    List<ConnectRequest> findByCategoryAndIsDeletedFalseOrderBySupportCountDescIdDesc(String category);

    List<ConnectRequest> findByRequesterUserIdAndIsDeletedFalseOrderByIdDesc(Long requesterUserId);
}
