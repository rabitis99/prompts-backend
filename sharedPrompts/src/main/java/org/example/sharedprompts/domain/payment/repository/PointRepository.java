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
     * 사용자의 현재 포인트 잔액 조회
     * 최신 거래의 balance를 조회하거나, amount의 합계를 계산
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Point p WHERE p.user.id = :userId AND p.expired = false")
    Optional<BigDecimal> getCurrentBalance(@Param("userId") Long userId);
    
    /**
     * 사용자의 최신 포인트 잔액 조회 (가장 최근 거래의 balance)
     */
    @Query("SELECT p.balance FROM Point p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<BigDecimal> findLatestBalances(@Param("userId") Long userId);

    /**
     * 사용자의 포인트 내역 조회
     */
    List<Point> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * 결제와 연관된 포인트 조회
     */
    List<Point> findByPaymentId(Long paymentId);

    /**
     * 만료 예정 포인트 조회
     */
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId AND p.expired = false AND p.expiredAt <= :expiryDate")
    List<Point> findExpiringPoints(@Param("userId") Long userId, @Param("expiryDate") LocalDateTime expiryDate);

    @Query("SELECT p FROM Point p " +
            "WHERE p.paymentId = :paymentId " +
            "AND p.user.id = :userId " +
            "ORDER BY p.createdAt DESC")
    List<Point> findByPaymentIdAndUserId(@Param("paymentId") Long paymentId,
                                         @Param("userId") Long userId);

    /**
     * 사용자의 마지막 포인트 내역 조회 (가장 최근 생성된 Point)
     * 동시성 제어를 위해 사용됩니다.
     * 
     * @param userId 사용자 ID
     * @return 가장 최근 생성된 Point (없으면 빈 리스트)
     */
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId ORDER BY p.createdAt DESC, p.id DESC")
    List<Point> findLatestPointByUserId(@Param("userId") Long userId);
}

