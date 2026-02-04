package org.example.sharedprompts.domain.payment.repository.point;

import org.example.sharedprompts.domain.payment.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 Repository
 */
public interface PointRepository extends JpaRepository<Point, Long>, CustomPointRepository {

    /**
     * 사용자의 현재 포인트 잔액 조회
     * 최신 거래의 balance를 조회하거나, amount의 합계를 계산
     * COALESCE로 항상 0 이상의 값을 반환하므로 Optional이 불필요합니다.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Point p WHERE p.user.id = :userId AND p.expired = false")
    BigDecimal getCurrentBalance(@Param("userId") Long userId);
    
    /**
     * 사용자의 최신 포인트 잔액 조회 (가장 최근 거래의 balance)
     */
    @Query("SELECT p.balance FROM Point p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<BigDecimal> findLatestBalances(@Param("userId") Long userId);

    /**
     * 만료 예정 포인트 조회
     */
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId AND p.expired = false AND p.expiredAt <= :expiryDate")
    List<Point> findExpiringPoints(@Param("userId") Long userId, @Param("expiryDate") LocalDateTime expiryDate);

    /**
     * 사용자의 마지막 포인트 내역 조회 (가장 최근 생성된 Point)
     * 동시성 제어를 위해 사용됩니다.
     *
     * @param userId 사용자 ID
     * @return 가장 최근 생성된 Point (없으면 빈 리스트)
     */
    @Query("SELECT p FROM Point p WHERE p.user.id = :userId ORDER BY p.createdAt DESC, p.id DESC")
    List<Point> findLatestPointByUserId(@Param("userId") Long userId);

    /**
     * 결제 ID와 포인트 타입으로 기존 포인트 내역 존재 여부 확인 (멱등성 체크)
     * 콜백 재시도 시 중복 포인트 적립/복구 방지용
     *
     * @param paymentId 결제 ID
     * @param type 포인트 타입
     * @return 해당 조건의 포인트 내역 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Point p WHERE p.payment.id = :paymentId AND p.type = :type")
    boolean existsByPaymentIdAndType(@Param("paymentId") Long paymentId, @Param("type") org.example.sharedprompts.domain.payment.enums.PointType type);

    /**
     * 결제 ID, 사용자 ID, 포인트 타입으로 기존 포인트 내역 존재 여부 확인 (멱등성 체크)
     * 소유자 검증을 포함한 안전한 멱등성 체크
     *
     * @param paymentId 결제 ID
     * @param userId 사용자 ID
     * @param type 포인트 타입
     * @return 해당 조건의 포인트 내역 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Point p WHERE p.payment.id = :paymentId AND p.user.id = :userId AND p.type = :type")
    boolean existsByPaymentIdAndUserIdAndType(@Param("paymentId") Long paymentId, @Param("userId") Long userId, @Param("type") org.example.sharedprompts.domain.payment.enums.PointType type);
}

