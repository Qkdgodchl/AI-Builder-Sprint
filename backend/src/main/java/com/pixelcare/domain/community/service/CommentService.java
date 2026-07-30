package com.pixelcare.domain.community.service;

import com.pixelcare.domain.community.dto.CommentCreateRequest;
import com.pixelcare.domain.community.dto.CommentResponse;
import com.pixelcare.domain.community.entity.Comment;
import com.pixelcare.domain.community.entity.Post;
import com.pixelcare.domain.community.repository.CommentRepository;
import com.pixelcare.domain.community.repository.PostRepository;
import com.pixelcare.global.error.ApiException;
import org.springframework.http.HttpStatus;
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

    public List<CommentResponse> getComments(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        return commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, String nickname, String badge) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));

        String finalNickname = (request.getAuthorNickname() != null && !request.getAuthorNickname().isBlank())
                ? request.getAuthorNickname()
                : (nickname != null ? nickname : "익명 용사");

        String finalBadge = (request.getAuthorBadge() != null && !request.getAuthorBadge().isBlank())
                ? request.getAuthorBadge()
                : (badge != null ? badge : "LV1_SEED");

        Comment comment = new Comment(postId, request.getContent(), finalNickname, finalBadge);
        Comment savedComment = commentRepository.save(comment);

        post.incrementCommentCount();

        return CommentResponse.fromEntity(savedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, String deletedBy) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));

        comment.markDeleted(deletedBy != null ? deletedBy : "USER_SYSTEM");

        postRepository.findById(comment.getPostId()).ifPresent(Post::decrementCommentCount);
    }
}
