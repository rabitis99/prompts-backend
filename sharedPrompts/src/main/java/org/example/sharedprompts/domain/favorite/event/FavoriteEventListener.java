package org.example.sharedprompts.domain.favorite.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.favorite.event.FavoriteEvent.*;
import org.example.sharedprompts.domain.favorite.service.FavoriteCountService;
import org.example.sharedprompts.domain.notification.processor.NotificationEventProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FavoriteEventListener {

    private final FavoriteCountService favoriteCountService;
    private final NotificationEventProcessor notificationEventProcessor;

    /** 프롬프트 즐겨찾기 추가 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromptFavorited(PromptFavorited event) {
        try {
            favoriteCountService.incrementPromptFavoriteCount(event.promptId());
        } catch (Exception e) {
            log.error("Failed to increment favorite count: {}", event, e);
        }
        
        // 알림 발행
        try {
            notificationEventProcessor.processPromptFavorited(event);
        } catch (Exception e) {
            log.error("Failed to process favorite notification: {}", event, e);
        }
    }

    /** 프롬프트 즐겨찾기 취소 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromptUnfavorited(PromptUnfavorited event) {
        try {
            favoriteCountService.decrementPromptFavoriteCount(event.promptId());
        } catch (Exception e) {
            log.error("Failed to decrement favorite count: {}", event, e);
        }
    }
}

