package com.pixelcare.domain.community.repository;

import com.pixelcare.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
}
