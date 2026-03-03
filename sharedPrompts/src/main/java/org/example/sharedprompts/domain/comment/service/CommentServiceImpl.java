package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.infrastructure.persistence.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.comment.request.CommentRequestDto;
import org.example.sharedprompts.dto.comment.request.CommentUpdateDto;
import org.example.sharedprompts.dto.comment.response.CommentResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PromptRepository promptRepository;
    private final UserRepository userRepository;
    private final CommentContentSanitizer commentContentSanitizer;
    private final CommentPermissionChecker commentPermissionChecker;
    private final CommentEventPublisher commentEventPublisher;

    @Override
    @Transactional
    public CommentResponseDto createComment(Long userId, Long promptId, CommentRequestDto requestDto) {
        User user = getUser(userId);
        Prompt prompt = getPrompt(promptId);
        Comment parent = resolveParent(requestDto.getParentId(), prompt);

        String sanitizedContent = commentContentSanitizer.sanitize(requestDto.getContent());
        Comment comment = requestDto.toEntity(user, prompt, parent, sanitizedContent);

        commentRepository.save(comment);

        commentEventPublisher.publishCreated(comment, parent != null ? parent.getId() : null);

        return CommentResponseDto.from(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByPrompt(Long promptId, Pageable pageable) {
        Prompt prompt = getPrompt(promptId);

        Page<Comment> rootComments = commentRepository.findRootCommentsByPrompt(prompt, pageable);

        return rootComments.map(CommentResponseDto::from);
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(Long userId, Long promptId, Long commentId, CommentUpdateDto updateDto) {
        User user = getUser(userId);
        Prompt prompt = getPrompt(promptId);
        Comment comment = getComment(commentId);

        commentPermissionChecker.check(user, prompt, comment);

        String sanitizedContent = commentContentSanitizer.sanitize(updateDto.getContent());
        updateDto.apply(comment, sanitizedContent);

        return CommentResponseDto.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long promptId, Long commentId) {
        User user = getUser(userId);
        Prompt prompt = getPrompt(promptId);
        Comment comment = getComment(commentId);

        commentPermissionChecker.check(user, prompt, comment);

        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

        commentRepository.delete(comment);

        commentEventPublisher.publishDeleted(promptId, parentId);
    }

    private Comment resolveParent(Long parentId, Prompt prompt) {
        if (parentId == null) return null;

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));

        if (!parent.getPrompt().equals(prompt)) {
            throw new ApiException(ErrorCode.COMMENT_NOT_BELONG_TO_PROMPT);
        }

        return parent;
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private Prompt getPrompt(Long id) {
        return promptRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROMPT_NOT_FOUND));
    }

    private Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
