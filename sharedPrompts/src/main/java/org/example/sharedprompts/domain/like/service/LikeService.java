package org.example.sharedprompts.domain.like.service;

public interface LikeService {
    void likePrompt(Long userId, Long promptId);
    void unlikePrompt(Long userId, Long promptId);
    void likeComment(Long userId, Long commentId);
    void unlikeComment(Long userId, Long commentId);
}
