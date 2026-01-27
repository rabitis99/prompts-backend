package org.example.sharedprompts.domain.statistics.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.event.PromptEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 통계 캐시 무효화 이벤트 리스너
 * 
 * <p>프롬프트 생성/삭제 시 통계 캐시를 즉시 무효화하여 데이터 일관성을 보장합니다.
 * 트랜잭션 커밋 후 처리하여 롤백 시 캐시 무효화가 발생하지 않도록 합니다.
 * 
 * <p>무효화되는 캐시 키:
 * <ul>
 *   <li>{@code "all"} - 전체 통계</li>
 *   <li>{@code "user"} - 사용자 통계</li>
 *   <li>{@code "prompt"} - 프롬프트 통계</li>
 *   <li>{@code "ai-call"} - AI 호출 통계</li>
 * </ul>
 * 
 * <p>개인 통계 캐시({@code "my:{userId}"})는 사용자별로 관리되므로
 * 프롬프트 생성/삭제 시 해당 사용자의 개인 통계 캐시도 함께 무효화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsCacheEventListener {

    private static final String CACHE_NAME = "statistics";
    
    // 통계 캐시 키 상수
    private static final String CACHE_KEY_ALL = "all";
    private static final String CACHE_KEY_USER = "user";
    private static final String CACHE_KEY_PROMPT = "prompt";
    private static final String CACHE_KEY_AI_CALL = "ai-call";

    private final CacheManager cacheManager;

    /**
     * 프롬프트 생성 이벤트 처리
     * 트랜잭션 커밋 후 통계 캐시 무효화
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromptCreated(PromptEvent.Created event) {
        log.debug("Prompt created event received: promptId={}, userId={}", 
                event.promptId(), event.userId());
        evictStatisticsCache();
        evictUserStatisticsCache(event.userId());
    }

    /**
     * 프롬프트 삭제 이벤트 처리
     * 트랜잭션 커밋 후 통계 캐시 무효화
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromptDeleted(PromptEvent.Deleted event) {
        log.debug("Prompt deleted event received: promptId={}, userId={}", 
                event.promptId(), event.userId());
        evictStatisticsCache();
        evictUserStatisticsCache(event.userId());
    }

    /**
     * 통계 캐시 무효화
     * 모든 통계 관련 캐시 키를 제거합니다.
     * 
     * <p>에러 발생 시에도 로그만 남기고 예외를 전파하지 않아
     * 이벤트 처리 실패가 다른 비즈니스 로직에 영향을 주지 않도록 합니다.
     */
    private void evictStatisticsCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.warn("Statistics cache not found: cacheName={}", CACHE_NAME);
            return;
        }

        try {
            cache.evictIfPresent(CACHE_KEY_ALL);
            cache.evictIfPresent(CACHE_KEY_USER);
            cache.evictIfPresent(CACHE_KEY_PROMPT);
            cache.evictIfPresent(CACHE_KEY_AI_CALL);
            
            log.debug("Statistics cache evicted after prompt change: keys=[{}, {}, {}, {}]", 
                    CACHE_KEY_ALL, CACHE_KEY_USER, CACHE_KEY_PROMPT, CACHE_KEY_AI_CALL);
        } catch (Exception e) {
            log.error("Failed to evict statistics cache: {}", e.getMessage(), e);
            // 예외를 전파하지 않아 이벤트 처리 실패가 다른 로직에 영향을 주지 않도록 함
        }
    }

    /**
     * 개인 통계 캐시 무효화
     * 프롬프트 생성/삭제 시 해당 사용자의 개인 통계 캐시를 무효화합니다.
     * 
     * <p>개인 통계는 사용자의 프롬프트 수를 기반으로 계산되므로,
     * 프롬프트 생성/삭제 시 해당 사용자의 캐시("my:{userId}")도 함께 무효화해야 합니다.
     * 
     * <p>에러 발생 시에도 로그만 남기고 예외를 전파하지 않아
     * 이벤트 처리 실패가 다른 비즈니스 로직에 영향을 주지 않도록 합니다.
     * 
     * @param userId 사용자 ID
     */
    private void evictUserStatisticsCache(Long userId) {
        if (userId == null) {
            log.warn("Cannot evict user statistics cache: userId is null");
            return;
        }

        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.warn("Statistics cache not found: cacheName={}", CACHE_NAME);
            return;
        }

        try {
            String cacheKey = "my:" + userId.toString();
            cache.evictIfPresent(cacheKey);
            log.debug("User statistics cache evicted: userId={}, cacheKey={}", userId, cacheKey);
        } catch (Exception e) {
            log.error("Failed to evict user statistics cache: userId={}, error={}", 
                    userId, e.getMessage(), e);
            // 예외를 전파하지 않아 이벤트 처리 실패가 다른 로직에 영향을 주지 않도록 함
        }
    }
}

