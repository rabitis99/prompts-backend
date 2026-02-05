package org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.cashback;

import org.example.sharedprompts.domain.payment.domain.entity.Cashback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 캐시백 Repository
 */
public interface CashbackRepository extends JpaRepository<Cashback, Long>, CustomCashbackRepository {

    /**
     * 사용자의 미지급 캐시백 총액 조회
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cashback c WHERE c.user.id = :userId AND c.paid = false")
    BigDecimal getUnpaidCashbackTotal(@Param("userId") Long userId);

    /**
     * 결제와 연관된 캐시백 조회
     */
    Optional<Cashback> findByPaymentId(Long paymentId);

    /**
     * 전체 미지급 캐시백 총액 조회 (관리자용)
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cashback c WHERE c.paid = false")
    BigDecimal getAllUnpaidCashbackTotal();
}

