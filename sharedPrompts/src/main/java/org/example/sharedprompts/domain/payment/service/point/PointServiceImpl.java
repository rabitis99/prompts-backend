package org.example.sharedprompts.domain.payment.service.point;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.Point;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.PointRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.PointUseRequestDto;
import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 포인트 서비스 구현체
 * 
 * 멀티 서버 환경에서 동시성 문제를 해결하기 위해 Redisson 분산락을 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;
    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "point:lock:";
    private static final long LOCK_WAIT_TIME = 10; // 락 대기 시간 (초)
    private static final long LOCK_LEASE_TIME = 30; // 락 유지 시간 (초)

    // ============ Public Methods ============

    @Override
    @Transactional
    public void accumulatePoints(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 포인트 적립률 적용
        BigDecimal pointAmount = paymentAmount.multiply(BigDecimal.valueOf(paymentProperties.getPointRate()))
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
    public void usePoints(Long userId, BigDecimal amount, String description) {
        executeWithLock(userId, () -> {
            doUsePoints(userId, amount, description);
            return null;
        });
    }

    @Override
    public BigDecimal getCurrentBalance(Long userId) {
        return pointRepository.getCurrentBalance(userId)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public PointBalanceResponseDto getBalanceDetail(Long userId) {
        BigDecimal currentBalance = getCurrentBalance(userId);
        BigDecimal availableBalance = calculateAvailableBalance(userId);
        BigDecimal expiringSoon = calculateExpiringSoon(userId);
        
        return PointBalanceResponseDto.from(userId, currentBalance, availableBalance, expiringSoon);
    }

    @Override
    public List<PointResponseDto> getPointHistory(Long userId) {
        return pointRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PointResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<PointResponseDto> getPointsByPayment(Long paymentId, Long userId) {
        List<Point> points = pointRepository.findByPaymentIdAndUserId(paymentId, userId);

        if (points.isEmpty()) {
            throw new ApiException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return points.stream()
                .map(PointResponseDto::from)
                .collect(Collectors.toList());
    }

    // ============ Lock Management ============

    /**
     * 분산락을 획득한 후 작업을 실행합니다.
     */
    private <T> T executeWithLock(Long userId, Supplier<T> task) {
        String lockKey = getLockKey(userId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire lock for user: {}", userId);
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            try {
                return task.get();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for user: {}", userId, e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing task with lock for user: {}", userId, e);
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
        User user = getUser(userId);
        BigDecimal lastBalance = getLastBalance(userId);
        BigDecimal newBalance = lastBalance.add(pointAmount);

        Point point = Point.builder()
                .user(user)
                .paymentId(paymentId)
                .amount(pointAmount)
                .type("PAYMENT")
                .description("결제 포인트 적립")
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
     * 사용 가능한 잔액 계산
     */
    private BigDecimal calculateAvailableBalance(Long userId) {
        return pointRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(p -> !p.isExpired() && p.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(Point::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

