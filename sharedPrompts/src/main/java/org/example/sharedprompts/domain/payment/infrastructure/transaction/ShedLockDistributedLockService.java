package org.example.sharedprompts.domain.payment.infrastructure.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.example.sharedprompts.global.exception.ApiException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShedLockDistributedLockService implements DistributedLockService {

    private final LockProvider lockProvider;
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
            if (e instanceof ObjectOptimisticLockingFailureException) {
                log.debug("락 내 작업에서 낙관적 락 충돌 발생: lockKey={}, message={}", lockKey, e.getMessage());
            } else if (e instanceof ApiException) {
                log.warn("락 내 작업에서 비즈니스 예외 발생: lockKey={}, message={}", lockKey, e.getMessage());
            } else {
                log.error("락 내 작업 실행 중 오류: lockKey={}", lockKey, e);
            }
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
        if (activeLocks.containsKey(lockKey)) {
            log.warn("이미 보유 중인 락 키로 tryLock 시도: lockKey={}", lockKey);
            return false;
        }

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

