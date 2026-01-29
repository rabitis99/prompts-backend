package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.Cashback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 캐시백 Repository
 */
public interface CashbackRepository extends JpaRepository<Cashback, Long> {

    /**
     * Compute the total unpaid cashback amount for the specified user.
     *
     * @param userId the identifier of the user whose unpaid cashback total is calculated
     * @return an Optional containing the total unpaid cashback amount for the user; empty if no matching records
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cashback c WHERE c.user.id = :userId AND c.paid = false")
    Optional<BigDecimal> getUnpaidCashbackTotal(@Param("userId") Long userId);

    /**
 * Retrieves cashback records for a user ordered by creation time descending.
 *
 * @param userId the ID of the user whose cashback history to retrieve
 * @return a list of Cashback entries for the user, ordered newest first
 */
    List<Cashback> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
 * Finds the cashback associated with the given payment ID.
 *
 * @param paymentId the ID of the payment whose cashback to retrieve
 * @return an Optional containing the cashback for the payment, or empty if none exists
 */
    Optional<Cashback> findByPaymentId(Long paymentId);

    /**
 * Retrieve unpaid cashback entries for the given user ordered by creation time ascending.
 *
 * @param userId the ID of the user whose unpaid cashback entries to retrieve
 * @return a list of unpaid Cashback records for the user ordered by createdAt ascending
 */
    List<Cashback> findByUser_IdAndPaidFalseOrderByCreatedAtAsc(Long userId);
}
