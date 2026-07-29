package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.CommentCreateRequest;
import com.pixelcare.domain.community.dto.CommentResponse;
import com.pixelcare.domain.community.entity.Comment;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.repository.CommentRepository;
import com.pixelcare.domain.community.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    public List<CommentResponse> getComments(Long postId) {
        // 게시글 존재 검증
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다. id=" + postId);
        }
        return commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::new)
                .toList();
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, String authorNickname, String authorBadge) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + postId));

        Comment comment = new Comment(
                post,
                authorNickname,
                authorBadge,
                request.getContent(),
                request.getParentCommentId()
        );

        Comment savedComment = commentRepository.save(comment);
        post.updateCommentCount(1); // 댓글 수 증가

        return new CommentResponse(savedComment);
    }

    /**
     * 댓글 소프트 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, String deletedBy) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다. id=" + commentId));

        if (comment.isDeleted()) {
            throw new IllegalArgumentException("이미 삭제된 댓글입니다. id=" + commentId);
        }

        comment.markDeleted(deletedBy);
        comment.getPost().updateCommentCount(-1); // 댓글 수 감소
    }
}
