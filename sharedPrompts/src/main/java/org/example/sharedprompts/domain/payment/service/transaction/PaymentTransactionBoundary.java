package org.example.sharedprompts.domain.payment.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.service.lock.DistributedLockService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * 결제 트랜잭션 경계 관리 서비스
 *
 * <p><strong>책임:</strong>
 * <ul>
 *   <li>트랜잭션 경계 관리: 락 획득 → 트랜잭션 시작 → 작업 수행 → 커밋 → 락 해제 순서 보장</li>
 *   <li>트랜잭션 전파 전략 중앙 관리: REQUIRES_NEW 전략 사용</li>
 *   <li>Service 레이어의 Infrastructure 책임 제거</li>
 * </ul>
 *
 * <p><strong>사용 목적:</strong>
 * 문서(PAYMENT_DOMAIN_ARCHITECTURE_ANALYSIS.md)의 개선 제안에 따라
 * Service 레이어에서 트랜잭션/락 관리 책임을 분리하여:
 * <ul>
 *   <li>트랜잭션 전파 전략 변경 시 비즈니스 로직 수정 불필요</li>
 *   <li>테스트 시 트랜잭션 없이 비즈니스 로직만 테스트 가능</li>
 *   <li>책임 명확화: Service는 비즈니스 로직만, Infrastructure는 기술적 관심사만</li>
 * </ul>
 *
 * <p><strong>트랜잭션 순서:</strong>
 * 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제
 * 이를 통해 락이 해제된 후 트랜잭션이 커밋되기 전에 다른 스레드가 락을 획득하는 문제를 방지합니다.
 *
 * @see org.example.sharedprompts.domain.payment.service.core.PaymentServiceImpl
 * @see org.example.sharedprompts.domain.payment.service.admin.AdminPaymentServiceImpl
 */
@Slf4j
@Component
public class PaymentTransactionBoundary {

    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

    /**
     * REQUIRES_NEW 전파로 설정된 TransactionTemplate을 생성합니다.
     * 상위 트랜잭션과 독립적으로 실행되어 락 획득 → 트랜잭션 시작 순서를 보장합니다.
     */
    public PaymentTransactionBoundary(
            DistributedLockService distributedLockService,
            PlatformTransactionManager transactionManager) {
        this.distributedLockService = distributedLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 락 획득 후 트랜잭션 내에서 작업을 실행합니다.
     *
     * <p><strong>트랜잭션 순서:</strong>
     * 락 획득 → 트랜잭션 시작 → 작업 수행 → 트랜잭션 커밋 → 락 해제
     *
     * @param lockKey 락 키
     * @param task 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    public <T> T executeWithLockAndTransaction(String lockKey, Supplier<T> task) {
        return distributedLockService.executeWithLock(lockKey, () ->
                transactionTemplate.execute(status -> task.get())
        );
    }

    /**
     * 트랜잭션만 실행합니다 (락 없이).
     *
     * <p>후처리 작업 등 락이 필요하지 않은 경우 사용합니다.
     *
     * @param task 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    public <T> T executeInTransaction(Supplier<T> task) {
        return transactionTemplate.execute(status -> task.get());
    }

    /**
     * 락만 획득합니다 (트랜잭션 없이).
     *
     * <p>트랜잭션이 필요하지 않은 작업에 락만 필요한 경우 사용합니다.
     *
     * @param lockKey 락 키
     * @param task 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return distributedLockService.executeWithLock(lockKey, task);
    }
}

