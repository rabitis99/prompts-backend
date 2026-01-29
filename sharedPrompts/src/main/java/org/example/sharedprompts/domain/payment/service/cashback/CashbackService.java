package org.example.sharedprompts.domain.payment.service.cashback;

import java.math.BigDecimal;

/**
 * 캐시백 서비스 인터페이스
 */
public interface CashbackService {

    /**
 * Accrues cashback for a user based on a specific payment.
 *
 * @param userId        identifier of the user who will receive the cashback
 * @param paymentId     identifier of the payment used to calculate cashback
 * @param paymentAmount payment amount used to determine the cashback value
 */
    void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount);

    /**
 * Dispense the specified cashback entry to the given user.
 *
 * @param userId     identifier of the user who will receive the cashback
 * @param cashbackId identifier of the cashback entry to be paid out
 */
    void payCashback(Long userId, Long cashbackId);

    /**
 * Get the total unpaid cashback amount for the specified user.
 *
 * @param userId the identifier of the user whose unpaid cashback total is requested
 * @return the total unpaid cashback amount for the user as a BigDecimal
 */
    BigDecimal getUnpaidCashbackTotal(Long userId);
}
