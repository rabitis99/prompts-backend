package org.example.sharedprompts.domain.payment.service.point;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import java.time.Instant;
import org.example.sharedprompts.domain.payment.Point;
import org.example.sharedprompts.domain.payment.config.RewardProperties;
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
import org.springframework.transaction.annotation.Transactional;

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
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;
    private final RewardProperties rewardProperties;
    private final UserRepository userRepository;
    /**
     * LockProvider 주입 (@Primary로 지정된 메인 LockProvider 사용)
     * fallback이 활성화되어 있으면 fallbackLockProvider를, 없으면 lockProvider를 사용
     */
    private final LockProvider lockProvider;

    private static final String LOCK_PREFIX = "point:lock:";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofSeconds(30); // 락 최대 유지 시간
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofMillis(100); // 락 최소 유지 시간 (분산 환경에서 너무 빨리 해제되는 것 방지)

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

        executeWithLock(userId, () -> {
            doAccumulatePoints(userId, paymentId, pointAmount);
            return null;
        });
    }

    @Override
    @Transactional
    public void addPointsDirectly(Long userId, Long paymentId, BigDecimal pointAmount, String description) {
        if (pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // 적립할 포인트가 없으면 종료
        }

        executeWithLock(userId, () -> {
            doAddPointsDirectly(userId, paymentId, pointAmount, description);
            return null;
        });
    }

    @Override
    @Transactional
    public void usePoints(Long userId, BigDecimal amount, String description) {
        executeWithLock(userId, () -> {
            doUsePoints(userId, amount, description);
            return null;
        });
    }

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
            return task.get();
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
        doAddPointsDirectly(userId, paymentId, pointAmount, "결제 포인트 적립");
    }

    /**
     * 직접 포인트 적립 비즈니스 로직
     */
    private void doAddPointsDirectly(Long userId, Long paymentId, BigDecimal pointAmount, String description) {
        User user = getUser(userId);
        BigDecimal lastBalance = getLastBalance(userId);
        BigDecimal newBalance = lastBalance.add(pointAmount);

        Point point = Point.builder()
                .user(user)
                .paymentId(paymentId)
                .amount(pointAmount)
                .type("PAYMENT")
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
     */
    private BigDecimal getLastBalance(Long userId) {
        List<Point> latestPoints = pointRepository.findLatestPointByUserId(userId);
        return latestPoints.isEmpty() 
                ? BigDecimal.ZERO 
                : latestPoints.get(0).getBalance();
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

