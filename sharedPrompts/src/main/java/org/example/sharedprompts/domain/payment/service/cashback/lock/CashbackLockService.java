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
 *
 * <p><strong>알려진 제한사항 - 동시성 제어 일관성:</strong>
 * - CashbackLockService: ShedLock (LockProvider) 사용
 * - PointServiceImpl: ShedLock (LockProvider) 사용 (코드 중복)
 * - WebhookIdempotencyService: Redis setIfAbsent 직접 사용
 * - 동일한 기능을 서로 다른 방식으로 구현하여 유지보수 어려움
 *
 * <p><strong>권장 개선사항:</strong>
 * - 통일된 DistributedLockService 인터페이스 도입
 * - 락 타임아웃, 재시도, 에러 처리 정책을 중앙에서 관리
 * - 락 획득 실패 시 동작 일관되게 정의 (재시도 vs 즉시 실패)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackLockService {

    private final LockProvider lockProvider;

    private static final String LOCK_PREFIX = "cashback:lock:";
    private static final String PAYMENT_LOCK_PREFIX = "cashback:payment:lock:";
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
     * paymentId 기반 분산락을 획득한 후 작업을 실행합니다.
     * 캐시백 적립 시 동일 결제에 대한 중복 적립을 방지합니다.
     */
    public <T> T executeWithLockForPayment(Long paymentId, Supplier<T> task) {
        String lockName = getPaymentLockKey(paymentId);
        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                lockName,
                LOCK_AT_MOST_FOR,
                LOCK_AT_LEAST_FOR
        );

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
        if (lock.isEmpty()) {
            log.warn("Failed to acquire lock for payment: {}", paymentId);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            return task.get();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing task with lock for payment: {}", paymentId, e);
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

    /**
     * paymentId 기반 락 키 생성
     */
    public String getPaymentLockKey(Long paymentId) {
        return PAYMENT_LOCK_PREFIX + paymentId;
    }
}

