package org.example.sharedprompts.domain.like.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.like.event.LikeEvent.*;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LikeEventListener {

    private final LikeCountService likeCountService;

    /** 프롬프트 좋아요 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromptLiked(PromptLiked event) {
        try {
            likeCountService.incrementPromptLikeCount(event.promptId());
        } catch (Exception e) {
            log.error("Failed to increment prompt like count: {}", event, e);
        }
    }

    /** 프롬프트 좋아요 취소 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromptUnliked(PromptUnliked event) {
        try {
            likeCountService.decrementPromptLikeCount(event.promptId());
        } catch (Exception e) {
            log.error("Failed to decrement prompt like count: {}", event, e);
        }
    }

    /** 댓글 좋아요 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentLiked(CommentLiked event) {
        try {
            likeCountService.incrementCommentLikeCount(event.commentId());
        } catch (Exception e) {
            log.error("Failed to increment comment like count: {}", event, e);
        }
    }

    /** 댓글 좋아요 취소 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentUnliked(CommentUnliked event) {
        try {
            likeCountService.decrementCommentLikeCount(event.commentId());
        } catch (Exception e) {
            log.error("Failed to decrement comment like count: {}", event, e);
        }
    }
}
