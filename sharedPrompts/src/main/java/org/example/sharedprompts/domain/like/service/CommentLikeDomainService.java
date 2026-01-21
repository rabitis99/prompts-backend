package org.example.sharedprompts.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.like.CommentLike;
import org.example.sharedprompts.domain.like.CommentLikeId;
import org.example.sharedprompts.domain.like.repository.CommentLikeRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeDomainService {

    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CommentLike like(Long userId, Long commentId) {
        validateUserExists(userId);
        validateCommentExists(commentId);

        CommentLikeId id = new CommentLikeId(commentId, userId);

        User user = userRepository.getReferenceById(userId);
        Comment comment = commentRepository.getReferenceById(commentId);

        CommentLike commentLike = CommentLike.builder()
                .id(id)
                .user(user)
                .comment(comment)
                .build();

        return saveCommentLikeOrThrow(commentLike);
    }

    @Transactional
    public void unlike(Long userId, Long commentId) {
        CommentLikeId id = new CommentLikeId(commentId, userId);

        if (!commentLikeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.COMMENT_LIKE_NOT_FOUND);
        }

        commentLikeRepository.deleteById(id);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateCommentExists(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private CommentLike saveCommentLikeOrThrow(CommentLike commentLike) {
        try {
            return commentLikeRepository.save(commentLike);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.COMMENT_ALREADY_LIKED);
        }
    }
}


