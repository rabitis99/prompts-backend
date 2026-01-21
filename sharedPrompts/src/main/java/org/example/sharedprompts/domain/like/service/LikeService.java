package org.example.sharedprompts.domain.like.service;

import org.example.sharedprompts.dto.like.response.CommentLikeResponseDto;
import org.example.sharedprompts.dto.like.response.PromptLikeResponseDto;

public interface LikeService {
    PromptLikeResponseDto likePrompt(Long userId, Long promptId);
    PromptLikeResponseDto unlikePrompt(Long userId, Long promptId);
    CommentLikeResponseDto likeComment(Long userId, Long commentId);
    CommentLikeResponseDto unlikeComment(Long userId, Long commentId);
    PromptLikeResponseDto checkPromptLike(Long userId, Long promptId);
    CommentLikeResponseDto checkCommentLike(Long userId, Long commentId);
}
