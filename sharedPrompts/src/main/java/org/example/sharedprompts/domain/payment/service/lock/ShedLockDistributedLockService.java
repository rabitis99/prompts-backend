package org.example.sharedprompts.domain.payment.service.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * ShedLock 기반 분산 락 서비스 구현체
 *
 * <p><strong>특징:</strong>
 * <ul>
 *   <li>ShedLock의 LockProvider를 사용하여 분산 환경에서 락 관리</li>
 *   <li>기본 락 타임아웃: 30초</li>
 *   <li>기본 최소 락 유지 시간: 100ms (분산 환경에서 너무 빨리 해제되는 것 방지)</li>
 *   <li>락 획득 실패 시 LockAcquisitionException 발생</li>
 * </ul>
 *
 * <p><strong>사용되는 곳:</strong>
 * <ul>
 *   <li>CashbackFacade: 캐시백 적립/지급</li>
 *   <li>PointServiceImpl: 포인트 적립/차감</li>
 *   <li>WebhookHandler: 웹훅 처리 멱등성</li>
 * </ul>
 *
 * @see DistributedLockService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShedLockDistributedLockService implements DistributedLockService {

    private final LockProvider lockProvider;

    // tryLock으로 획득한 락을 저장 (unlock 시 사용)
    private final Map<String, SimpleLock> activeLocks = new ConcurrentHashMap<>();

    private static final Duration DEFAULT_LOCK_AT_MOST_FOR = Duration.ofSeconds(30);
    private static final Duration DEFAULT_LOCK_AT_LEAST_FOR = Duration.ofMillis(100);

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, DEFAULT_LOCK_AT_MOST_FOR, DEFAULT_LOCK_AT_LEAST_FOR, task);
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration lockAtMostFor, Duration lockAtLeastFor, Supplier<T> task) {
        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                lockKey,
                lockAtMostFor,
                lockAtLeastFor
        );

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);

        if (lock.isEmpty()) {
            log.warn("락 획득 실패: lockKey={}", lockKey);
            throw new LockAcquisitionException(lockKey);
        }

        log.debug("락 획득 성공: lockKey={}", lockKey);

        try {
            return task.get();
        } catch (Exception e) {
            log.error("락 내 작업 실행 중 오류: lockKey={}", lockKey, e);
            throw e;
        } finally {
            try {
                lock.get().unlock();
                log.debug("락 해제 완료: lockKey={}", lockKey);
            } catch (Exception e) {
                log.error("락 해제 중 오류: lockKey={}", lockKey, e);
            }
        }
    }

    @Override
    public boolean tryLock(String lockKey, Duration lockAtMostFor) {
        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                lockKey,
                lockAtMostFor,
                DEFAULT_LOCK_AT_LEAST_FOR
        );

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);

        if (lock.isPresent()) {
            activeLocks.put(lockKey, lock.get());
            log.debug("tryLock 성공: lockKey={}", lockKey);
            return true;
        }

        log.debug("tryLock 실패: lockKey={}", lockKey);
        return false;
    }

    @Override
    public void unlock(String lockKey) {
        SimpleLock lock = activeLocks.remove(lockKey);
        if (lock != null) {
            try {
                lock.unlock();
                log.debug("unlock 완료: lockKey={}", lockKey);
            } catch (Exception e) {
                log.error("unlock 중 오류: lockKey={}", lockKey, e);
            }
        } else {
            log.warn("unlock 대상 락이 없음: lockKey={}", lockKey);
        }
    }
}
