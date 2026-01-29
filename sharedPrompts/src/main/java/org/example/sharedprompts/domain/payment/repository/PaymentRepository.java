package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.Payment;
import org.example.sharedprompts.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
 * Finds a payment by its external payment identifier.
 *
 * @param externalPaymentId the external identifier assigned to the payment
 * @return an Optional containing the Payment if found, otherwise an empty Optional
 */
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    /**
     * Finds a payment by its id and the owning user's id.
     *
     * @param id     the payment's id
     * @param userId the id of the user who owns the payment
     * @return       an Optional containing the Payment if found, `Optional.empty()` otherwise
     */
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.user.id = :userId")
    Optional<Payment> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Count payments for a user on a specific date with the given status.
     *
     * The comparison uses only the calendar date portion of `date` and `Payment.createdAt`.
     *
     * @param userId the user's ID to filter payments
     * @param date the date whose calendar date is compared to payment creation timestamps
     * @param status the payment status to filter by
     * @return the number of payments matching the user, date, and status
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND DATE(p.createdAt) = DATE(:date) AND p.status = :status")
    long countByUserIdAndDateAndStatus(
            @Param("userId") Long userId,
            @Param("date") LocalDateTime date,
            @Param("status") PaymentStatus status
    );

    /**
     * Count a user's payments with status SUCCESS created on the current date.
     *
     * @param userId the user's id whose today's successful payments are counted
     * @return the number of payments with `SUCCESS` status created today for the specified user
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND DATE(p.createdAt) = CURRENT_DATE AND p.status = 'SUCCESS'")
    long countTodaySuccessfulPayments(@Param("userId") Long userId);

    /**
 * Retrieve all payments for the specified user ordered by creation time descending.
 *
 * @param userId the id of the user whose payments to retrieve
 * @return a list of Payment entities for the user ordered by newest first
 */
    List<Payment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
 * Retrieve payments with the given status ordered by creation time descending.
 *
 * @param status the payment status to filter by
 * @return a list of Payment entities that match the specified status, ordered from newest to oldest
 */
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    /**
     * Finds payments in PENDING status with retryCount less than the specified maximum, ordered by creation time ascending.
     *
     * @param maxRetryCount the exclusive upper bound for retryCount; payments with retryCount less than this value are returned
     * @return a list of Payment entities eligible for retry, ordered by createdAt ascending
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.retryCount < :maxRetryCount ORDER BY p.createdAt ASC")
    List<Payment> findPendingPaymentsForRetry(@Param("maxRetryCount") int maxRetryCount);
}
