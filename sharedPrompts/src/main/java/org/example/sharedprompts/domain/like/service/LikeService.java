package org.example.sharedprompts.domain.like.service;

import org.example.sharedprompts.dto.like.response.CommentLikeResponseDto;
import org.example.sharedprompts.dto.like.response.PromptLikeResponseDto;

public interface LikeService {
    void likePrompt(Long userId, Long promptId);
    void unlikePrompt(Long userId, Long promptId);
    void likeComment(Long userId, Long commentId);
    void unlikeComment(Long userId, Long commentId);
    PromptLikeResponseDto checkPromptLike(Long userId, Long promptId);
    CommentLikeResponseDto checkCommentLike(Long userId, Long commentId);
}
