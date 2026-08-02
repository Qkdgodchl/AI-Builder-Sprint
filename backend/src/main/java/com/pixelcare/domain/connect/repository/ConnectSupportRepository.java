package com.pixelcare.domain.connect.repository;

import com.pixelcare.domain.connect.entity.ConnectSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectSupportRepository extends JpaRepository<ConnectSupport, Long> {

    Optional<ConnectSupport> findByRequestIdAndUserId(Long requestId, Long userId);

    List<ConnectSupport> findByUserId(Long userId);
}
