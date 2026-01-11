package org.example.sharedprompts.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.comment.event.CommentEvent;
import org.example.sharedprompts.domain.comment.repository.CommentRepository;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.dto.comment.request.CommentRequestDto;
import org.example.sharedprompts.dto.comment.request.CommentUpdateDto;
import org.example.sharedprompts.dto.comment.response.CommentResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PromptRepository promptRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CommentResponseDto createComment(Long userId, Long promptId, CommentRequestDto requestDto) {
        User user = getUser(userId);
        Prompt prompt = getPrompt(promptId);

        Comment parent = null;

        if (requestDto.getParentId() != null) {
            parent = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND));

            if (!parent.getPrompt().equals(prompt)) {
                throw new ApiException(ErrorCode.COMMENT_NOT_BELONG_TO_PROMPT);
            }
        }

        Comment comment = requestDto.toEntity(user, prompt, parent);

        commentRepository.save(comment);

        eventPublisher.publishEvent(new CommentEvent.Created(
                promptId,
                parent != null ? parent.getId() : null
        ));

        return CommentResponseDto.from(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPrompt(Long promptId) {
        Prompt prompt = getPrompt(promptId);

        List<Comment> rootComments = commentRepository.findAllByPrompt(prompt);

        return rootComments.stream()
                .map(CommentResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(Long userId, Long promptId, Long commentId, CommentUpdateDto commentUpdateDto) {
        User user = getUser(userId);
        Prompt prompt = getPrompt(promptId);
        Comment comment = getComment(commentId);

        checkCommentPermission(user, prompt, comment);

        commentUpdateDto.apply(comment);

        return CommentResponseDto.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long promptId, Long commentId) {
        User user = getUser(userId);
        Prompt prompt = getPrompt(promptId);
        Comment comment = getComment(commentId);

        checkCommentPermission(user, prompt, comment);

        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

        commentRepository.delete(comment);

        eventPublisher.publishEvent(new CommentEvent.Deleted(
                promptId,
                parentId
        ));
    }

    private void checkCommentPermission(User user, Prompt prompt, Comment comment) {
        if (!comment.getPrompt().equals(prompt)) {
            throw new ApiException(ErrorCode.COMMENT_NOT_BELONG_TO_PROMPT);
        }

        boolean isAuthor = comment.getUser().equals(user);
        boolean isPromptAuthor = prompt.getAuthor().equals(user);
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!(isAuthor || isPromptAuthor || isAdmin)) {
            throw new ApiException(ErrorCode.COMMENT_FORBIDDEN);
        }
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
