package com.pixelcare.domain.community.repository;

import com.pixelcare.domain.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);

    Page<Post> findByAuthorUserIdAndIsDeletedFalse(Long authorUserId, Pageable pageable);

    Optional<Post> findByIdAndIsDeletedFalse(Long id);
}
