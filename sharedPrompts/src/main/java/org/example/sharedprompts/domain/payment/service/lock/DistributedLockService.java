package org.example.sharedprompts.domain.payment.service.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 분산 락 서비스 인터페이스
 *
 * <p><strong>도입 배경 (2024-02-02):</strong>
 * 기존에 동시성 제어가 여러 방식으로 산재되어 있었습니다:
 * <ul>
 *   <li>CashbackLockService: ShedLock (LockProvider) 사용</li>
 *   <li>PointServiceImpl: ShedLock (LockProvider) 사용 (코드 중복)</li>
 *   <li>WebhookIdempotencyService: Redis setIfAbsent 직접 사용</li>
 * </ul>
 *
 * <p>이 인터페이스는 분산 락 획득/해제를 위한 통일된 추상화를 제공하여:
 * <ul>
 *   <li>코드 중복 제거</li>
 *   <li>락 정책(타임아웃, 재시도) 중앙 관리</li>
 *   <li>테스트 용이성 향상 (인메모리 구현체 제공 가능)</li>
 *   <li>인프라 변경 시 영향 최소화</li>
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 * <pre>{@code
 * distributedLockService.executeWithLock("payment:123", () -> {
 *     // 락 내에서 안전하게 실행할 로직
 *     return processPayment();
 * });
 * }</pre>
 *
 * @see ShedLockDistributedLockService
 */
public interface DistributedLockService {

    /**
     * 기본 설정으로 분산 락을 획득한 후 작업을 실행합니다.
     *
     * @param lockKey 락 키
     * @param task 락 내에서 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 실행 결과
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    <T> T executeWithLock(String lockKey, Supplier<T> task);

    /**
     * 커스텀 설정으로 분산 락을 획득한 후 작업을 실행합니다.
     *
     * @param lockKey 락 키
     * @param lockAtMostFor 락 최대 유지 시간 (타임아웃)
     * @param lockAtLeastFor 락 최소 유지 시간 (분산 환경에서 너무 빨리 해제되는 것 방지)
     * @param task 락 내에서 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 실행 결과
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    <T> T executeWithLock(String lockKey, Duration lockAtMostFor, Duration lockAtLeastFor, Supplier<T> task);

    /**
     * 반환값이 없는 작업을 락 내에서 실행합니다.
     *
     * @param lockKey 락 키
     * @param task 락 내에서 실행할 작업
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    default void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, () -> {
            task.run();
            return null;
        });
    }

    /**
     * 락 획득을 시도합니다 (논블로킹).
     *
     * <p><strong>주의:</strong> 이 메서드로 획득한 락은 반드시 {@link #unlock(String)}을
     * 호출하여 해제해야 합니다. 가능하면 {@link #executeWithLock(String, Supplier)}을 사용하세요.
     *
     * @param lockKey 락 키
     * @param lockAtMostFor 락 최대 유지 시간
     * @return 락 획득 성공 여부
     */
    boolean tryLock(String lockKey, Duration lockAtMostFor);

    /**
     * 락을 해제합니다.
     *
     * @param lockKey 락 키
     */
    void unlock(String lockKey);

    /**
     * 락 키 생성 유틸리티
     *
     * @param prefix 락 키 프리픽스 (예: "payment", "cashback")
     * @param id 고유 식별자
     * @return 생성된 락 키
     */
    default String createLockKey(String prefix, Long id) {
        return prefix + ":" + id;
    }

    /**
     * 락 획득 실패 시 발생하는 예외
     */
    class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String lockKey) {
            super("Failed to acquire lock: " + lockKey);
        }

        public LockAcquisitionException(String lockKey, Throwable cause) {
            super("Failed to acquire lock: " + lockKey, cause);
        }
    }
}
