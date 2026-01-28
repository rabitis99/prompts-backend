package org.example.sharedprompts.domain.prompt.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptUsageCountService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 프롬프트 조회 이벤트 리스너
 * 
 * <p>AFTER_COMMIT 단계에서 실행되어 조회 API의 readOnly 트랜잭션과 분리됩니다.
 * <p>Redis에 조회수를 증가시키며, Redis 장애 시에도 조회 API는 실패하지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptViewedEventListener {

    private final PromptUsageCountService promptUsageCountService;

    /**
     * 프롬프트 조회 이벤트 처리
     * 
     * <p>트랜잭션 커밋 후 실행되므로 조회 API의 readOnly 트랜잭션과 완전히 분리됩니다.
     * <p>Redis에 조회수를 증가시키며, Redis 장애 시 로그만 남기고 조용히 무시합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromptViewed(PromptEvent.Viewed event) {
        try {
            promptUsageCountService.incrementUsageCount(event.promptId());
            log.debug("Incremented usage count in Redis. promptId={}, viewerId={}", 
                    event.promptId(), event.viewerId());
        } catch (Exception e) {
            // Redis 장애 시에도 조회 API는 이미 성공했으므로 조용히 무시
            log.warn("Failed to increment usage count after prompt view. promptId={}, viewerId={}, error={}", 
                    event.promptId(), event.viewerId(), e.getMessage());
        }
    }
}

