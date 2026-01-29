package org.example.sharedprompts.domain.payment.service.cashback;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.payment.Cashback;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.CashbackRepository;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 캐시백 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashbackServiceImpl implements CashbackService {

    private final CashbackRepository cashbackRepository;
    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;

    /**
     * Accumulates cashback for a user's payment when the computed cashback is greater than zero.
     *
     * The cashback amount is computed as paymentAmount × configured cashback rate, rounded down to 2 decimal places,
     * and persisted as an unpaid Cashback record when the result is greater than zero.
     *
     * @param userId        the id of the user who made the payment
     * @param paymentId     the id of the payment for which cashback is calculated
     * @param paymentAmount the original payment amount used to calculate cashback
     * @throws ApiException if the user with the given id is not found (ErrorCode.USER_NOT_FOUND)
     */
    @Override
    @Transactional
    public void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount) {
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
        }
    }

    /**
     * Marks the specified cashback as paid after validating existence, ownership, and that it has not already been paid.
     *
     * @param userId     the ID of the user attempting the payout
     * @param cashbackId the ID of the cashback to mark as paid
     * @throws ApiException if the cashback does not exist (NOT_FOUND), if the user is not the owner (FORBIDDEN), or if the cashback is already paid (BAD_REQUEST)
     */
    @Override
    @Transactional
    public void payCashback(Long userId, Long cashbackId) {
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

        // 실제 지급 로직은 여기에 구현 (예: 계좌 이체, 포인트 전환 등)
        cashback.markAsPaid();
        cashbackRepository.save(cashback);
    }

    /**
     * Get the total unpaid cashback amount for the specified user.
     *
     * @param userId the ID of the user to query
     * @return the total unpaid cashback amount for the user, or BigDecimal.ZERO if none exists
     */
    @Override
    public BigDecimal getUnpaidCashbackTotal(Long userId) {
        return cashbackRepository.getUnpaidCashbackTotal(userId)
                .orElse(BigDecimal.ZERO);
    }
}
