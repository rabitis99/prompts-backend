package org.example.sharedprompts.domain.payment.repository;

import org.example.sharedprompts.domain.payment.Cashback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 캐시백 Repository
 */
public interface CashbackRepository extends JpaRepository<Cashback, Long>, CustomCashbackRepository {

    /**
     * 사용자의 미지급 캐시백 총액 조회
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cashback c WHERE c.user.id = :userId AND c.paid = false")
    Optional<BigDecimal> getUnpaidCashbackTotal(@Param("userId") Long userId);

    /**
     * 사용자의 캐시백 내역 조회
     */
    List<Cashback> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * 결제와 연관된 캐시백 조회
     */
    Optional<Cashback> findByPaymentId(Long paymentId);

    /**
     * 미지급 캐시백 목록 조회
     */
    List<Cashback> findByUser_IdAndPaidFalseOrderByCreatedAtAsc(Long userId);

    /**
     * 사용자의 캐시백 내역 조회 (페이징)
     */
    Page<Cashback> findByUser_Id(Long userId, Pageable pageable);

    /**
     * 미지급 캐시백 목록 조회 (페이징)
     */
    Page<Cashback> findByUser_IdAndPaidFalse(Long userId, Pageable pageable);
}

