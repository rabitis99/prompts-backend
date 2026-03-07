package org.example.sharedprompts.domain.payment.service.point;

import lombok.extern.slf4j.Slf4j;

import org.example.sharedprompts.domain.payment.domain.entity.Point;
import org.example.sharedprompts.domain.payment.config.properties.RewardProperties;
import org.example.sharedprompts.domain.payment.domain.entity.Payment;
import org.example.sharedprompts.domain.payment.domain.enums.PointType;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PointJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.transaction.DistributedLockService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PointUseRequestDto;
import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;

/**
 * 포인트 서비스 구현체
 *
 * <p><strong>동시성 제어 전략:</strong>
 * <ul>
 *   <li>포인트 적립 (accumulatePoints, addPointsDirectly): 분산 락 미사용
 *       <ul>
 *         <li>멱등성 체크로 중복 적립 방지 (paymentId + userId + PointType 조합)</li>
 *         <li>데이터베이스 트랜잭션으로 일관성 보장</li>
 *         <li>락 타임아웃 문제 방지를 위해 락 제거</li>
 *       </ul>
 *   </li>
 *   <li>포인트 사용 (usePoints): ShedLock 분산락 사용
 *       <ul>
 *         <li>동시 사용 요청 시 잔액 부족 방지</li>
 *         <li>잔액 차감의 정확성 보장</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>알려진 제한사항 - 잔액 관리 방식:</strong>
 * - Point 엔티티의 balance 필드에 누적 잔액 저장 (getLastBalance 사용)
 * - getCurrentBalance는 별도 집계 쿼리 사용
 * - 두 메서드가 다른 방식으로 잔액 계산하여 일시적 불일치 가능성 존재
 * - 락 외부에서 getCurrentBalance 호출 시 부정확할 수 있음
 *
 * <p><strong>권장 개선사항:</strong>
 * - balance 필드 제거하고 항상 집계 쿼리로 잔액 계산 (단일 진실 공급원)
 * - 또는 User 엔티티에 pointBalance 필드 추가하여 신뢰할 수 있는 단일 소스로 관리
 * - Point 엔티티는 이력만 저장하고 잔액 계산은 별도 집계 테이블 사용
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PointServiceImpl implements PointService {

    private final PointJpaAdapter pointJpaAdapter;
    private final RewardProperties rewardProperties;
    private final UserRepository userRepository;
    private final PaymentJpaAdapter paymentJpaAdapter;
    /**
     * 분산락 추상화 계층 (테스트/인프라 분리 목적)
     */
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

    public PointServiceImpl(
            PointJpaAdapter pointJpaAdapter,
            RewardProperties rewardProperties,
            UserRepository userRepository,
            PaymentJpaAdapter paymentJpaAdapter,
            DistributedLockService distributedLockService,
            PlatformTransactionManager transactionManager) {
        this.pointJpaAdapter = pointJpaAdapter;
        this.rewardProperties = rewardProperties;
        this.userRepository = userRepository;
        this.paymentJpaAdapter = paymentJpaAdapter;
        this.distributedLockService = distributedLockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // REQUIRES_NEW 전파로 설정하여 상위 트랜잭션과 독립적으로 실행
        // 락 획득 → 트랜잭션 시작 순서를 보장
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static final String LOCK_PREFIX = "point:lock:";

    // ============ Public Methods ============

    @Override
    @Transactional
    public void accumulatePoints(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 포인트 적립률 적용
        BigDecimal pointAmount = paymentAmount.multiply(rewardProperties.getPointRate())
                .setScale(0, RoundingMode.DOWN);

        if (pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 적립할 포인트가 없으면 종료
        }

        // 분산 락 제거: 멱등성 체크로 중복 적립 방지, 데이터베이스 트랜잭션으로 일관성 보장
        doAccumulatePoints(userId, paymentId, pointAmount);
    }

    @Override
    @Transactional
    public void addPointsDirectly(Long userId, Long paymentId, BigDecimal pointAmount, PointType type, String description) {
        if (pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 적립할 포인트가 없으면 종료
        }

        // 분산 락 제거: 멱등성 체크로 중복 적립 방지, 데이터베이스 트랜잭션으로 일관성 보장
        doAddPointsDirectly(userId, paymentId, pointAmount, type, description);
    }

    @Override
    public void usePoints(Long userId, BigDecimal amount, String description) {
        executeWithLock(userId, () -> {
            doUsePoints(userId, amount, description);
            return null;
        });
    }

    /**
     * 현재 포인트 잔액 조회 (집계 쿼리 사용)
     *
     * <p><strong>주의:</strong> 성능을 위해 분산락을 사용하지 않습니다.
     * 동시 포인트 업데이트 중에는 일시적으로 부정확한 값을 반환할 수 있습니다.
     * 정확한 잔액이 필요한 경우 락이 적용된 메서드 내부에서 getLastBalance()를 사용하세요.
     */
    @Override
    public BigDecimal getCurrentBalance(Long userId) {
        return pointJpaAdapter.getCurrentBalance(userId);
    }

    @Override
    public PointBalanceResponseDto getBalanceDetail(Long userId) {
        BigDecimal currentBalance = getCurrentBalance(userId);
        BigDecimal expiringSoon = calculateExpiringSoon(userId);
        
        // availableBalance는 currentBalance와 동일 (사용 포인트를 반영한 실제 잔액)
        return PointBalanceResponseDto.from(userId, currentBalance, currentBalance, expiringSoon);
    }

    @Override
    public Page<PointResponseDto> getPointHistory(Long userId, Pageable pageable) {
        return pointJpaAdapter.findByUserIdWithFetchJoin(userId, pageable)
                .map(PointResponseDto::from);
    }

    @Override
    public Page<PointResponseDto> getPointsByPayment(Long paymentId, Long userId, Pageable pageable) {
        Page<Point> points = pointJpaAdapter.findByPaymentIdAndUserIdWithFetchJoin(paymentId, userId, pageable);
        return points.map(PointResponseDto::from);
    }

    @Override
    public Page<PointResponseDto> getPointsByPaymentForAdmin(Long paymentId, Pageable pageable) {
        // 관리자는 소유권 검증 없이 모든 결제의 포인트 조회 가능
        Page<Point> points = pointJpaAdapter.findByPaymentIdWithFetchJoin(paymentId, pageable);
        return points.map(PointResponseDto::from);
    }

    // ============ Lock Management ============

    /**
     * 분산락을 획득한 후 작업을 실행합니다.
     * 락 내부에서 트랜잭션을 관리하여 락 해제 전에 트랜잭션이 커밋되도록 보장합니다.
     */
    private <T> T executeWithLock(Long userId, Supplier<T> task) {
        String lockName = getLockKey(userId);
        try {
            // 락 획득 → (REQUIRES_NEW) 트랜잭션 시작 → 작업 수행 → 커밋 → 락 해제
            return distributedLockService.executeWithLock(lockName, () -> transactionTemplate.execute(status -> task.get()));
        } catch (DistributedLockService.LockAcquisitionException e) {
            log.warn("포인트 락 획득 실패: userId={}, lockKey={}", userId, lockName);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("포인트 락 내 작업 실행 중 오류: userId={}, lockKey={}", userId, lockName, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String getLockKey(Long userId) {
        return LOCK_PREFIX + userId;
    }

    // ============ Business Logic ============

    /**
     * 포인트 적립 비즈니스 로직
     */
    private void doAccumulatePoints(Long userId, Long paymentId, BigDecimal pointAmount) {
        doAddPointsDirectly(userId, paymentId, pointAmount, PointType.PAYMENT, "결제 포인트 적립");
    }

    /**
     * 직접 포인트 적립 비즈니스 로직
     *
     * <p>paymentId가 있는 경우 멱등성 체크를 수행하여 중복 적립을 방지합니다.
     * 콜백 재시도 등으로 동일 결제에 대해 여러 번 호출되어도 한 번만 적립됩니다.
     *
     * <p><strong>동시성 제어:</strong>
     * - 분산 락을 사용하지 않음 (락 타임아웃 문제 방지)
     * - 멱등성 체크로 중복 적립 방지
     * - 데이터베이스 트랜잭션으로 일관성 보장
     *
     * <p><strong>보안:</strong> 소유자 검증을 멱등성 체크보다 먼저 수행하여
     * 다른 사용자의 paymentId로 멱등성 체크를 우회하는 것을 방지합니다.
     */
    private void doAddPointsDirectly(Long userId, Long paymentId, BigDecimal pointAmount, PointType type, String description) {
        User user = getUser(userId);
        BigDecimal lastBalance = getLastBalance(userId);
        BigDecimal newBalance = lastBalance.add(pointAmount);

        // Payment 엔티티 조회 및 검증 (paymentId가 있는 경우만)
        Payment payment = null;
        if (paymentId != null) {
            payment = paymentJpaAdapter.findById(paymentId)
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 결제 ID입니다: paymentId=" + paymentId));
            
            // 결제 소유자 검증: paymentId가 제공되면 반드시 해당 사용자의 결제여야 함
            // 멱등성 체크보다 먼저 수행하여 보안 강화
            if (!payment.getUser().getId().equals(userId)) {
                throw new ApiException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "결제 소유자와 사용자 정보가 일치하지 않습니다: paymentId=" + paymentId + ", userId=" + userId);
            }
            
            // 멱등성 체크: paymentId + userId + PointType 조합이 이미 존재하면 스킵
            // 소유자 검증 후 수행하여 다른 사용자의 paymentId로 우회 불가
            if (pointJpaAdapter.existsByPaymentIdAndUserIdAndType(paymentId, userId, type)) {
                log.info("포인트 적립 스킵 (이미 처리됨): userId={}, paymentId={}, type={}", userId, paymentId, type);
                return;
            }
        }

        Point point = Point.builder()
                .user(user)
                .payment(payment)
                .amount(pointAmount)
                .type(type)
                .description(description)
                .balance(newBalance)
                .expired(false)
                .build();

        pointJpaAdapter.save(point);
    }

    /**
     * 포인트 사용 비즈니스 로직
     */
    private void doUsePoints(Long userId, BigDecimal amount, String description) {
        User user = getUser(userId);
        BigDecimal lastBalance = getLastBalance(userId);

        validateSufficientBalance(lastBalance, amount);

        BigDecimal newBalance = lastBalance.subtract(amount);
        Point point = createUsePoint(user, amount, description, newBalance);
        
        pointJpaAdapter.save(point);
    }

    /**
     * 사용자 조회
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 마지막 잔액 조회
     *
     * <p><strong>주의:</strong> Point 엔티티의 balance 필드를 사용하여 잔액 조회
     * - getCurrentBalance()와 다른 방식으로 계산하여 일시적 불일치 가능
     * - 분산락 내부에서만 호출되어야 정확성 보장
     * - 권장: balance 필드 대신 항상 집계 쿼리 사용 (향후 개선 필요)
     */
    private BigDecimal getLastBalance(Long userId) {
        List<Point> latestPoints = pointJpaAdapter.findLatestPointByUserId(userId);
        if (latestPoints.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return latestPoints.get(0).getBalance();
    }

    /**
     * 잔액 부족 검증
     */
    private void validateSufficientBalance(BigDecimal balance, BigDecimal amount) {
        // 포인트 사용 금액은 양수만 허용
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "포인트 사용 금액은 0보다 커야 합니다.");
        }
        // 잔액 부족 검증
        if (balance.compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.POINT_INSUFFICIENT);
        }
    }

    /**
     * 포인트 사용 엔티티 생성
     */
    private Point createUsePoint(User user, BigDecimal amount, String description, BigDecimal balance) {
        PointUseRequestDto requestDto = PointUseRequestDto.builder()
                .amount(amount)
                .description(description)
                .build();

        return requestDto.toPointBuilder(user, balance).build();
    }

    /**
     * 곧 만료될 포인트 계산
     */
    private BigDecimal calculateExpiringSoon(Long userId) {
        LocalDateTime expiryDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(30);
        return pointJpaAdapter.findExpiringPoints(userId, expiryDate)
                .stream()
                .filter(p -> !p.isExpired() && p.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Point::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

