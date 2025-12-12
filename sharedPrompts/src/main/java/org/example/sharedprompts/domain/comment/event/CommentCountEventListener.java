package org.example.sharedprompts.domain.comment.event;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.comment.service.CommentCountService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentCountEventListener {

    private final CommentCountService commentCountService;

    // ------------------ 댓글 생성 이벤트 처리 ------------------
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentEvent.Created event) {
        commentCountService.incrementCommentCount(event.promptId());
        if (event.parentId() != null) {
            commentCountService.incrementReplyCount(event.parentId());
        }
    }

    // ------------------ 댓글 삭제 이벤트 처리 ------------------
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentDeleted(CommentEvent.Deleted event) {
        commentCountService.decrementCommentCount(event.promptId());
        if (event.parentId() != null) {
            commentCountService.decrementReplyCount(event.parentId());
        }
    }
}
