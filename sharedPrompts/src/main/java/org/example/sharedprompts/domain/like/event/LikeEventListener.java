package org.example.sharedprompts.domain.like.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.like.event.LikeEvent.*;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LikeEventListener {

    private final LikeCountService likeCountService;

    /** 프롬프트 좋아요 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromptLiked(PromptLiked event) {
        likeCountService.incrementPromptLikeCount(event.promptId());
    }

    /** 프롬프트 좋아요 취소 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromptUnliked(PromptUnliked event) {
        likeCountService.decrementPromptLikeCount(event.promptId());
    }

    /** 댓글 좋아요 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentLiked(CommentLiked event) {
        likeCountService.incrementCommentLikeCount(event.commentId());
    }

    /** 댓글 좋아요 취소 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentUnliked(CommentUnliked event) {
        likeCountService.decrementCommentLikeCount(event.commentId());
    }
}
