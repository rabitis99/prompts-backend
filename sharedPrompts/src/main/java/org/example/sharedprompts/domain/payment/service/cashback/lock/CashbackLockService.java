package org.example.sharedprompts.domain.payment.service.cashback.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 캐시백 락 관리 서비스
 * 
 * <p>단일 책임: 분산 락 관리만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackLockService {

    private final LockProvider lockProvider;

    private static final String LOCK_PREFIX = "cashback:lock:";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofSeconds(30); // 락 최대 유지 시간
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofMillis(100); // 락 최소 유지 시간 (분산 환경에서 너무 빨리 해제되는 것 방지)

    /**
     * 분산락을 획득한 후 작업을 실행합니다.
     */
    public <T> T executeWithLock(Long cashbackId, Supplier<T> task) {
        String lockName = getLockKey(cashbackId);
        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                lockName,
                LOCK_AT_MOST_FOR,
                LOCK_AT_LEAST_FOR
        );

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
        if (lock.isEmpty()) {
            log.warn("Failed to acquire lock for cashback: {}", cashbackId);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            return task.get();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing task with lock for cashback: {}", cashbackId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            lock.get().unlock();
        }
    }

    /**
     * 락 키 생성
     */
    public String getLockKey(Long cashbackId) {
        return LOCK_PREFIX + cashbackId;
    }
}

