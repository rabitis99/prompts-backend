package org.example.sharedprompts.domain.payment.service.cashback.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.DistributedLockService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 캐시백 락 관리 서비스
 *
 * <p>단일 책임: 캐시백 도메인의 분산 락 관리
 *
 * <p><strong>2024-02-02 개선:</strong>
 * 기존에 LockProvider를 직접 사용하던 방식에서 통일된 DistributedLockService를
 * 사용하도록 리팩토링되었습니다. 이를 통해:
 * <ul>
 *   <li>코드 중복 제거 (PointServiceImpl과 동일한 락 로직 공유)</li>
 *   <li>락 정책(타임아웃, 재시도) 중앙 관리</li>
 *   <li>테스트 용이성 향상</li>
 * </ul>
 *
 * @see DistributedLockService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackLockService {

    private final DistributedLockService distributedLockService;

    private static final String LOCK_PREFIX = "cashback:lock:";
    private static final String PAYMENT_LOCK_PREFIX = "cashback:payment:lock:";

    /**
     * 분산락을 획득한 후 작업을 실행합니다.
     */
    public <T> T executeWithLock(Long cashbackId, Supplier<T> task) {
        String lockKey = getLockKey(cashbackId);

        try {
            return distributedLockService.executeWithLock(lockKey, task);
        } catch (DistributedLockService.LockAcquisitionException e) {
            log.warn("캐시백 락 획득 실패: cashbackId={}", cashbackId);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "캐시백 처리 중 동시성 오류가 발생했습니다.");
        }
    }

    /**
     * paymentId 기반 분산락을 획득한 후 작업을 실행합니다.
     * 캐시백 적립 시 동일 결제에 대한 중복 적립을 방지합니다.
     */
    public <T> T executeWithLockForPayment(Long paymentId, Supplier<T> task) {
        String lockKey = getPaymentLockKey(paymentId);

        try {
            return distributedLockService.executeWithLock(lockKey, task);
        } catch (DistributedLockService.LockAcquisitionException e) {
            log.warn("결제 기반 캐시백 락 획득 실패: paymentId={}", paymentId);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "캐시백 적립 중 동시성 오류가 발생했습니다.");
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

