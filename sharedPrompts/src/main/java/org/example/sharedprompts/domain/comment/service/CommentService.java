package org.example.sharedprompts.domain.comment.service;

import org.example.sharedprompts.dto.comment.request.CommentRequestDto;
import org.example.sharedprompts.dto.comment.request.CommentUpdateDto;
import org.example.sharedprompts.dto.comment.response.CommentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponseDto createComment(Long userId, Long promptId, CommentRequestDto requestDto);

    Page<CommentResponseDto> getCommentsByPrompt(Long promptId, Pageable pageable);

    CommentResponseDto updateComment(Long userId, Long promptId, Long commentId, CommentUpdateDto commentUpdateDto);

    void deleteComment(Long userId, Long promptId, Long commentId);
}