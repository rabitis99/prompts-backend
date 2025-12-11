package org.example.sharedprompts.domain.comment.service;

import org.example.sharedprompts.dto.comment.request.CommentRequestDto;
import org.example.sharedprompts.dto.comment.response.CommentResponseDto;

import java.util.List;

public interface CommentService {

    CommentResponseDto createComment(Long userId, Long promptId, CommentRequestDto requestDto);

    List<CommentResponseDto> getCommentsByPrompt(Long promptId);

    CommentResponseDto updateComment(Long userId, Long promptId, Long commentId, CommentRequestDto requestDto);

    void deleteComment(Long userId, Long promptId, Long commentId);
}