package org.example.sharedprompts.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.comment.event.CommentEvent;
import org.example.sharedprompts.domain.follow.event.FollowEvent;
import org.example.sharedprompts.domain.like.event.LikeEvent;
import org.example.sharedprompts.domain.notification.processor.NotificationEventProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 리스너
 * - Spring Events를 수신하여 NotificationEventProcessor에 위임
 * - 비즈니스 로직은 NotificationEventProcessor에서 처리
 * - 실제 알림 처리(저장, SSE 전송)는 NotificationConsumer에서 비동기로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationEventProcessor notificationEventProcessor;

    /**
     * 댓글 생성 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentEvent.Created event) {
        try {
            notificationEventProcessor.processCommentCreated(event);
        } catch (Exception e) {
            log.error("Failed to handle comment created event: {}", event, e);
        }
    }

    /**
     * 프롬프트 좋아요 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromptLiked(LikeEvent.PromptLiked event) {
        try {
            notificationEventProcessor.processPromptLiked(event);
        } catch (Exception e) {
            log.error("Failed to handle prompt liked event: {}", event, e);
        }
    }

    /**
     * 댓글 좋아요 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentLiked(LikeEvent.CommentLiked event) {
        try {
            notificationEventProcessor.processCommentLiked(event);
        } catch (Exception e) {
            log.error("Failed to handle comment liked event: {}", event, e);
        }
    }

    /**
     * 팔로우 요청 이벤트 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowRequested(FollowEvent.Requested event) {
        try {
            notificationEventProcessor.processFollowRequested(event);
        } catch (Exception e) {
            log.error("Failed to handle follow requested event: {}", event, e);
        }
    }
}

