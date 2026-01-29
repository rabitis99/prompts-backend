package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 포인트 Repository
 */
public interface PointRepository extends JpaRepository<Point, Long> {

    /**
     * Compute the user's current point balance from non-expired points.
     *
     * @param userId ID of the user whose balance to compute
     * @return an Optional containing the sum of amounts for the user's non-expired points; defaults to 0 when no matching points exist
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Point p WHERE p.user.id = :userId AND p.expired = false")
    Optional<BigDecimal> getCurrentBalance(@Param("userId") Long userId);
    
    /**
     * Retrieve the user's point balances ordered from newest to oldest.
     *
     * @param userId the ID of the user whose point balances to retrieve
     * @return a list of point balances (`BigDecimal`) ordered by creation time descending
     */
    @Query("SELECT p.balance FROM Point p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<BigDecimal> findLatestBalances(@Param("userId") Long userId);

    /**
 * Retrieves point records for a user ordered from newest to oldest.
 *
 * @param userId the identifier of the user
 * @return the list of Point entities for the given user ordered by `createdAt` descending
 */
    List<Point> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
 * Retrieves points associated with a specific payment.
 *
 * @param paymentId the ID of the payment whose associated points to retrieve
 * @return a list of Point entities linked to the specified payment, empty if none exist
 */
    List<Point> findByPaymentId(Long paymentId);

    /**
     * Finds non-expired points for the specified user that expire on or before the given date.
     *
     * @param userId     the ID of the user whose points to retrieve
     * @param expiryDate the cutoff date/time; points with `expiredAt` less than or equal to this value are included
     * @return           a list of matching Point entities; an empty list if none are found
     */
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId AND p.expired = false AND p.expiredAt <= :expiryDate")
    List<Point> findExpiringPoints(@Param("userId") Long userId, @Param("expiryDate") LocalDateTime expiryDate);
}
