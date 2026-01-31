package org.example.sharedprompts.domain.payment.service.point;

import org.example.sharedprompts.dto.payment.response.PointBalanceResponseDto;
import org.example.sharedprompts.dto.payment.response.PointResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * 포인트 서비스 인터페이스
 */
public interface PointService {

    /**
     * 포인트 적립
     */
    void accumulatePoints(Long userId, Long paymentId, BigDecimal paymentAmount);

    /**
     * 포인트 사용
     */
    void usePoints(Long userId, BigDecimal amount, String description);

    /**
     * 현재 포인트 잔액 조회
     */
    BigDecimal getCurrentBalance(Long userId);

    /**
     * 포인트 잔액 상세 조회 (만료 예정 포인트 포함)
     */
    PointBalanceResponseDto getBalanceDetail(Long userId);

    /**
     * 포인트 내역 조회
     */
    Page<PointResponseDto> getPointHistory(Long userId, Pageable pageable);

    /**
     * 결제와 연관된 포인트 조회
     */
    Page<PointResponseDto> getPointsByPayment(Long paymentId, Long userId, Pageable pageable);

    /**
     * 결제와 연관된 포인트 조회 (관리자용 - 소유권 검증 없음)
     */
    Page<PointResponseDto> getPointsByPaymentForAdmin(Long paymentId, Pageable pageable);
}

