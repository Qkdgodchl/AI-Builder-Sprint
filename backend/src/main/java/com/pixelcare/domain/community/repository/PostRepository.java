package com.pixelcare.domain.community.repository;

import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsDeletedFalse(Pageable pageable);

    Page<Post> findByCategoryAndIsDeletedFalse(PostCategory category, Pageable pageable);

    Optional<Post> findByIdAndIsDeletedFalse(Long id);
}
