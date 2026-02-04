package org.example.sharedprompts.domain.payment.service.point;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import java.time.Instant;
import org.example.sharedprompts.domain.payment.Point;
import org.example.sharedprompts.domain.payment.config.RewardProperties;
import org.example.sharedprompts.domain.payment.enums.PointType;
import org.example.sharedprompts.domain.payment.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.repository.point.PointRepository;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 포인트 서비스 구현체
 *
 * 멀티 서버 환경에서 동시성 문제를 해결하기 위해 ShedLock 분산락을 사용합니다.
 *
 * <p><strong>알려진 제한사항 - 잔액 관리 방식:</strong>
 * - Point 엔티티의 balance 필드에 누적 잔액 저장 (getLastBalance 사용)
 * - getCurrentBalance는 별도 집계 쿼리 사용
 * - 두 메서드가 다른 방식으로 잔액 계산하여 일시적 불일치 가능성 존재
 * - 분산락으로 동시성 제어하지만, 락 외부에서 getCurrentBalance 호출 시 부정확할 수 있음
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

    private final PointRepository pointRepository;
    private final RewardProperties rewardProperties;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    /**
     * LockProvider 주입 (@Primary로 지정된 메인 LockProvider 사용)
     * fallback이 활성화되어 있으면 fallbackLockProvider를, 없으면 lockProvider를 사용
     */
    private final LockProvider lockProvider;
    private final TransactionTemplate transactionTemplate;

    public PointServiceImpl(
            PointRepository pointRepository,
            RewardProperties rewardProperties,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            LockProvider lockProvider,
            PlatformTransactionManager transactionManager) {
        this.pointRepository = pointRepository;
        this.rewardProperties = rewardProperties;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.lockProvider = lockProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // REQUIRES_NEW 전파로 설정하여 상위 트랜잭션과 독립적으로 실행
        // 락 획득 → 트랜잭션 시작 순서를 보장
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static final String LOCK_PREFIX = "point:lock:";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofSeconds(30); // 락 최대 유지 시간
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofMillis(100); // 락 최소 유지 시간 (분산 환경에서 너무 빨리 해제되는 것 방지)

    // ============ Public Methods ============

    @Override
    public void accumulatePoints(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 포인트 적립률 적용
        BigDecimal pointAmount = paymentAmount.multiply(rewardProperties.getPointRate())
                .setScale(0, RoundingMode.DOWN);

        if (pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 적립할 포인트가 없으면 종료
        }

        executeWithLock(userId, () -> {
            doAccumulatePoints(userId, paymentId, pointAmount);
            return null;
        });
    }

    @Override
    public void addPointsDirectly(Long userId, Long paymentId, BigDecimal pointAmount, PointType type, String description) {
        if (pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 적립할 포인트가 없으면 종료
        }

        executeWithLock(userId, () -> {
            doAddPointsDirectly(userId, paymentId, pointAmount, type, description);
            return null;
        });
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
        return pointRepository.getCurrentBalance(userId);
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
        return pointRepository.findByUserIdWithFetchJoin(userId, pageable)
                .map(PointResponseDto::from);
    }

    @Override
    public Page<PointResponseDto> getPointsByPayment(Long paymentId, Long userId, Pageable pageable) {
        Page<Point> points = pointRepository.findByPaymentIdAndUserIdWithFetchJoin(paymentId, userId, pageable);
        return points.map(PointResponseDto::from);
    }

    @Override
    public Page<PointResponseDto> getPointsByPaymentForAdmin(Long paymentId, Pageable pageable) {
        // 관리자는 소유권 검증 없이 모든 결제의 포인트 조회 가능
        Page<Point> points = pointRepository.findByPaymentIdWithFetchJoin(paymentId, pageable);
        return points.map(PointResponseDto::from);
    }

    // ============ Lock Management ============

    /**
     * 분산락을 획득한 후 작업을 실행합니다.
     * 락 내부에서 트랜잭션을 관리하여 락 해제 전에 트랜잭션이 커밋되도록 보장합니다.
     */
    private <T> T executeWithLock(Long userId, Supplier<T> task) {
        String lockName = getLockKey(userId);
        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                lockName,
                LOCK_AT_MOST_FOR,
                LOCK_AT_LEAST_FOR
        );

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
        if (lock.isEmpty()) {
            log.warn("Failed to acquire lock for user: {}", userId);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            return transactionTemplate.execute(status -> task.get());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing task with lock for user: {}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            lock.get().unlock();
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
     */
    private void doAddPointsDirectly(Long userId, Long paymentId, BigDecimal pointAmount, PointType type, String description) {
        // 멱등성 체크: paymentId + PointType 조합이 이미 존재하면 스킵
        if (paymentId != null && pointRepository.existsByPaymentIdAndType(paymentId, type)) {
            log.info("포인트 적립 스킵 (이미 처리됨): userId={}, paymentId={}, type={}", userId, paymentId, type);
            return;
        }

        User user = getUser(userId);
        BigDecimal lastBalance = getLastBalance(userId);
        BigDecimal newBalance = lastBalance.add(pointAmount);

        // Payment 엔티티 조회 및 검증 (paymentId가 있는 경우만)
        org.example.sharedprompts.domain.payment.Payment payment = null;
        if (paymentId != null) {
            payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 결제 ID입니다: paymentId=" + paymentId));
            
            // 결제 소유자 검증: paymentId가 제공되면 반드시 해당 사용자의 결제여야 함
            if (!payment.getUser().getId().equals(userId)) {
                throw new ApiException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "결제 소유자와 사용자 정보가 일치하지 않습니다: paymentId=" + paymentId + ", userId=" + userId);
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

        pointRepository.save(point);
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
        
        pointRepository.save(point);
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
        List<Point> latestPoints = pointRepository.findLatestPointByUserId(userId);
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
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
        return pointRepository.findExpiringPoints(userId, expiryDate)
                .stream()
                .filter(p -> !p.isExpired() && p.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Point::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

