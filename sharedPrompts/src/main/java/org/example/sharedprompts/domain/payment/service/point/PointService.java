package org.example.sharedprompts.domain.payment.service.point;

import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 포인트 서비스 인터페이스
 */
public interface PointService {

    /**
 * Credit points to a user's account based on a completed payment.
 *
 * @param userId        the identifier of the user to receive points
 * @param paymentId     the identifier of the payment that generated the points
 * @param paymentAmount the payment amount used to determine the number of points to credit
 */
    void accumulatePoints(Long userId, Long paymentId, BigDecimal paymentAmount);

    /**
 * Deducts a specified amount of points from the user's balance.
 *
 * @param userId the ID of the user whose points will be deducted
 * @param amount the amount of points to deduct
 * @param description a brief reason or note for the deduction
 */
    void usePoints(Long userId, BigDecimal amount, String description);

    /**
 * Get the user's current total point balance.
 *
 * @param userId the id of the user whose balance to retrieve
 * @return the current point balance for the user as a BigDecimal
 */
    BigDecimal getCurrentBalance(Long userId);

    /**
 * Retrieve detailed point balance for a user, including points that are nearing expiration.
 *
 * @param userId the ID of the user whose balance is being queried
 * @return a PointBalanceResponseDto containing the user's total balance and a breakdown that includes expiring points
 */
    PointBalanceResponseDto getBalanceDetail(Long userId);

    /**
 * Retrieve the point transaction history for a user.
 *
 * @param userId the identifier of the user whose point history to retrieve
 * @return a list of PointResponseDto entries representing the user's point transactions
 */
    List<PointResponseDto> getPointHistory(Long userId);

    /**
 * Retrieve point entries associated with a specific payment.
 *
 * @param paymentId the identifier of the payment whose points are requested
 * @return a list of PointResponseDto representing points linked to the specified payment; an empty list if none are found
 */
    List<PointResponseDto> getPointsByPayment(Long paymentId);
}
