package org.example.sharedprompts.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.like.CommentLike;
import org.example.sharedprompts.domain.like.CommentLikeId;
import org.example.sharedprompts.domain.like.PromptLike;
import org.example.sharedprompts.domain.like.PromptLikeId;
import org.example.sharedprompts.domain.like.event.LikeEvent;
import org.example.sharedprompts.domain.like.repository.CommentLikeRepository;
import org.example.sharedprompts.domain.like.repository.PromptLikeRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final PromptLikeRepository likeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final CommentRepository commentRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void likePrompt(Long userId, Long promptId) {

        if (!promptRepository.existsById(promptId)) {
            throw new ApiException(ErrorCode.PROMPT_NOT_FOUND);
        }

        PromptLikeId id = new PromptLikeId(userId, promptId);

        if (likeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.PROMPT_ALREADY_LIKED);
        }

        User user = userRepository.getReferenceById(userId);
        Prompt prompt = promptRepository.getReferenceById(promptId);

        PromptLike promptLike = PromptLike.builder()
                .id(id)
                .user(user)
                .prompt(prompt)
                .build();

        likeRepository.save(promptLike);

        eventPublisher.publishEvent(new LikeEvent.PromptLiked(promptId));
    }

    @Override
    @Transactional
    public void unlikePrompt(Long userId, Long promptId) {

        PromptLikeId id = new PromptLikeId(userId, promptId);

        if (!likeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.PROMPT_LIKE_NOT_FOUND);
        }

        likeRepository.deleteById(id);

        eventPublisher.publishEvent(new LikeEvent.PromptUnliked(promptId));
    }

    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {

        // 댓글 존재 체크
        if (!commentRepository.existsById(commentId)) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }

        CommentLikeId id = new CommentLikeId(userId, commentId);

        if (commentLikeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.COMMENT_ALREADY_LIKED);
        }

        User user = userRepository.getReferenceById(userId);
        Comment comment = commentRepository.getReferenceById(commentId);

        CommentLike commentLike = CommentLike.builder()
                .id(id)
                .user(user)
                .comment(comment)
                .build();

        commentLikeRepository.save(commentLike);

        eventPublisher.publishEvent(new LikeEvent.CommentLiked(commentId));
    }

    @Override
    @Transactional
    public void unlikeComment(Long userId, Long commentId) {

        CommentLikeId id = new CommentLikeId(userId, commentId);

        if (!commentLikeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.COMMENT_LIKE_NOT_FOUND);
        }

        commentLikeRepository.deleteById(id);

        eventPublisher.publishEvent(new LikeEvent.CommentUnliked(commentId));
    }
}
