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
import org.example.sharedprompts.dto.like.response.CommentLikeResponseDto;
import org.example.sharedprompts.dto.like.response.PromptLikeResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
        validateUserExists(userId);
        validatePromptExists(promptId);

        PromptLikeId id = new PromptLikeId(promptId, userId);

        User user = userRepository.getReferenceById(userId);
        Prompt prompt = promptRepository.getReferenceById(promptId);

        PromptLike promptLike = PromptLike.builder()
                .id(id)
                .user(user)
                .prompt(prompt)
                .build();

        savePromptLikeOrThrow(promptLike);

        eventPublisher.publishEvent(new LikeEvent.PromptLiked(userId, promptId));
    }

    @Override
    @Transactional
    public void unlikePrompt(Long userId, Long promptId) {
        PromptLikeId id = new PromptLikeId(promptId, userId);

        if (!likeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.PROMPT_LIKE_NOT_FOUND);
        }

        likeRepository.deleteById(id);

        eventPublisher.publishEvent(new LikeEvent.PromptUnliked(promptId));
    }

    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
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

        saveCommentLikeOrThrow(commentLike);

        eventPublisher.publishEvent(new LikeEvent.CommentLiked(userId, commentId));
    }

    @Override
    @Transactional
    public void unlikeComment(Long userId, Long commentId) {
        CommentLikeId id = new CommentLikeId(commentId, userId);

        if (!commentLikeRepository.existsById(id)) {
            throw new ApiException(ErrorCode.COMMENT_LIKE_NOT_FOUND);
        }

        commentLikeRepository.deleteById(id);

        eventPublisher.publishEvent(new LikeEvent.CommentUnliked(commentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PromptLikeResponseDto checkPromptLike(Long userId, Long promptId) {
        PromptLikeId id = new PromptLikeId(promptId, userId);
        boolean isLiked = likeRepository.existsById(id);
        return PromptLikeResponseDto.from(isLiked);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentLikeResponseDto checkCommentLike(Long userId, Long commentId) {
        CommentLikeId id = new CommentLikeId(commentId, userId);
        boolean isLiked = commentLikeRepository.existsById(id);
        return CommentLikeResponseDto.from(isLiked);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validatePromptExists(Long promptId) {
        if (!promptRepository.existsById(promptId)) {
            throw new ApiException(ErrorCode.PROMPT_NOT_FOUND);
        }
    }

    private void validateCommentExists(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    /*
     * DataIntegrityViolationException 처리 전략:
     * 
     * DataIntegrityViolationException은 unique 제약과 FK 제약 위반을 모두 포함하지만,
     * 이 메서드에서는 PROMPT_ALREADY_LIKED 단일 에러로 매핑합니다.
     * 
     * 이유:
     * 1. FK 제약 위반은 대부분 사전 validate 단계(validateUserExists, validatePromptExists)에서
     *    차단되며, 남은 경우(예: 동시 삭제)도 클라이언트 관점에서는 동일한 "실패"로 취급됩니다.
     * 
     * 2. existsById를 통한 사전 중복 체크는 동시성 문제를 해결하지 못합니다:
     *    - Thread A: existsById() -> false
     *    - Thread B: existsById() -> false
     *    - Thread A: save() -> success
     *    - Thread B: save() -> duplicate key exception
     *    따라서 DB unique constraint를 최종 검증 수단으로 사용합니다.
     * 
     * 3. 이 접근은 Favorite 도메인과 동일한 전략을 유지하여 일관성을 보장합니다.
     */
    private void savePromptLikeOrThrow(PromptLike promptLike) {
        try {
            likeRepository.save(promptLike);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.PROMPT_ALREADY_LIKED);
        }
    }

    /*
     * DataIntegrityViolationException 처리 전략:
     * 
     * DataIntegrityViolationException은 unique 제약과 FK 제약 위반을 모두 포함하지만,
     * 이 메서드에서는 COMMENT_ALREADY_LIKED 단일 에러로 매핑합니다.
     * 
     * 이유:
     * 1. FK 제약 위반은 대부분 사전 validate 단계(validateUserExists, validateCommentExists)에서
     *    차단되며, 남은 경우(예: 동시 삭제)도 클라이언트 관점에서는 동일한 "실패"로 취급됩니다.
     * 
     * 2. existsById를 통한 사전 중복 체크는 동시성 문제를 해결하지 못합니다:
     *    - Thread A: existsById() -> false
     *    - Thread B: existsById() -> false
     *    - Thread A: save() -> success
     *    - Thread B: save() -> duplicate key exception
     *    따라서 DB unique constraint를 최종 검증 수단으로 사용합니다.
     * 
     * 3. 이 접근은 Favorite 도메인과 동일한 전략을 유지하여 일관성을 보장합니다.
     */
    private void saveCommentLikeOrThrow(CommentLike commentLike) {
        try {
            commentLikeRepository.save(commentLike);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.COMMENT_ALREADY_LIKED);
        }
    }
}
