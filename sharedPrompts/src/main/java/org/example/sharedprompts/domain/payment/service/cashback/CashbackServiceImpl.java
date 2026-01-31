package org.example.sharedprompts.domain.payment.service.cashback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.example.sharedprompts.domain.payment.Cashback;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.cashback.CashbackRepository;
import org.example.sharedprompts.domain.payment.service.point.PointService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 캐시백 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashbackServiceImpl implements CashbackService {

    private final CashbackRepository cashbackRepository;
    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final LockProvider lockProvider;

    private static final String LOCK_PREFIX = "cashback:lock:";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofSeconds(30); // 락 최대 유지 시간

    @Override
    public Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable) {
        return cashbackRepository.findByUserIdWithFetchJoin(customerId, pageable)
                .map(CashbackResponseDto::from);
    }

    @Override
    @Transactional
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount) {
        // 중복 캐시백 적립 방지: 동일 결제에 대한 캐시백이 이미 존재하는지 확인
        if (cashbackRepository.findByPaymentId(paymentId).isPresent()) {
            log.warn("이미 캐시백이 적립된 결제입니다: paymentId={}, userId={}", paymentId, userId);
            return;
        }

        // 캐시백 적립률 적용
        BigDecimal cashbackAmount = paymentAmount.multiply(BigDecimal.valueOf(paymentProperties.getCashbackRate()))
                .setScale(2, RoundingMode.DOWN); // 소수점 둘째 자리까지

        if (cashbackAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            
            Cashback cashback = Cashback.builder()
                    .user(user)
                    .paymentId(paymentId)
                    .amount(cashbackAmount)
                    .rate(BigDecimal.valueOf(paymentProperties.getCashbackRate()))
                    .paymentAmount(paymentAmount)
                    .description("결제 캐시백 적립")
                    .paid(false)
                    .build();

            cashbackRepository.save(cashback);
            log.info("캐시백 적립 완료: paymentId={}, userId={}, amount={}", paymentId, userId, cashbackAmount);
        }
    }

    @Override
    @Transactional
    public void payCashback(Long userId, Long cashbackId) {
        // 동시성 문제 방지를 위해 분산 락 적용
        executeWithLock(cashbackId, () -> {
            doPayCashback(userId, cashbackId);
            return null;
        });
    }

    /**
     * 캐시백 지급 비즈니스 로직 (락 내부에서 실행)
     */
    private void doPayCashback(Long userId, Long cashbackId) {
        Cashback cashback = cashbackRepository.findById(cashbackId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND,
                        "캐시백을 찾을 수 없습니다."
                ));

        if (!cashback.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "캐시백 지급 권한이 없습니다.");
        }

        if (cashback.isPaid()) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    "이미 지급된 캐시백입니다."
            );
        }

        // 캐시백을 포인트로 전환하여 지급
        BigDecimal cashbackAmount = cashback.getAmount();
        Long paymentId = cashback.getPaymentId();
        
        try {
            // 캐시백 금액을 포인트로 적립 (1원 = 1포인트로 전환)
            // paymentId는 캐시백이 발생한 원본 결제 ID를 사용
            pointService.addPointsDirectly(userId, paymentId, cashbackAmount, 
                    "캐시백 지급: " + cashbackAmount + "원");
            
            // 지급 완료 처리
            cashback.markAsPaid();
            cashbackRepository.save(cashback);
            
            log.info("캐시백 지급 완료: cashbackId={}, userId={}, amount={}, paymentId={}", 
                    cashbackId, userId, cashbackAmount, paymentId);
        } catch (Exception e) {
            log.error("캐시백 지급 실패: cashbackId={}, userId={}, amount={}, error={}", 
                    cashbackId, userId, cashbackAmount, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "캐시백 지급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public Page<CashbackResponseDto> getUnpaidCashbacks(Long customerId, Pageable pageable) {
        return cashbackRepository.findUnpaidByUserIdWithFetchJoin(customerId, pageable)
                .map(CashbackResponseDto::from);
    }

    @Override
    public BigDecimal getUnpaidCashbackTotal(Long userId) {
        return cashbackRepository.getUnpaidCashbackTotal(userId)
                .orElse(BigDecimal.ZERO);
    }

    // ============ 관리자용 메서드 ============

    @Override
    public BigDecimal getAllUnpaidCashbackTotal() {
        return cashbackRepository.getAllUnpaidCashbackTotal()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Page<CashbackResponseDto> getAllUnpaidCashbacks(Pageable pageable) {
        return cashbackRepository.findAllUnpaidWithFetchJoin(pageable)
                .map(CashbackResponseDto::from);
    }

    @Override
    @Transactional
    public void payCashbackForAdmin(Long cashbackId) {
        // 동시성 문제 방지를 위해 분산 락 적용
        executeWithLock(cashbackId, () -> {
            doPayCashbackForAdmin(cashbackId);
            return null;
        });
    }

    /**
     * 관리자용 캐시백 지급 비즈니스 로직 (락 내부에서 실행)
     */
    private void doPayCashbackForAdmin(Long cashbackId) {
        Cashback cashback = cashbackRepository.findById(cashbackId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND,
                        "캐시백을 찾을 수 없습니다."
                ));

        if (cashback.isPaid()) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    "이미 지급된 캐시백입니다."
            );
        }

        // 관리자는 소유권 검증 없이 지급 가능
        // 캐시백을 포인트로 전환하여 지급
        Long userId = cashback.getUser().getId();
        BigDecimal cashbackAmount = cashback.getAmount();
        Long paymentId = cashback.getPaymentId();
        
        try {
            // 캐시백 금액을 포인트로 적립 (1원 = 1포인트로 전환)
            pointService.addPointsDirectly(userId, paymentId, cashbackAmount, 
                    "캐시백 지급 (관리자): " + cashbackAmount + "원");
            
            // 지급 완료 처리
            cashback.markAsPaid();
            cashbackRepository.save(cashback);
            
            log.info("캐시백 지급 완료 (관리자): cashbackId={}, userId={}, amount={}, paymentId={}", 
                    cashbackId, userId, cashbackAmount, paymentId);
        } catch (Exception e) {
            log.error("캐시백 지급 실패 (관리자): cashbackId={}, userId={}, amount={}, error={}", 
                    cashbackId, userId, cashbackAmount, e.getMessage(), e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "캐시백 지급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ============ Lock Management ============

    /**
     * 분산락을 획득한 후 작업을 실행합니다.
     */
    private <T> T executeWithLock(Long cashbackId, Supplier<T> task) {
        String lockName = getLockKey(cashbackId);
        LockConfiguration lockConfig = new LockConfiguration(
                Instant.now(),
                lockName,
                LOCK_AT_MOST_FOR,
                Duration.ZERO
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

    private String getLockKey(Long cashbackId) {
        return LOCK_PREFIX + cashbackId;
    }
}

