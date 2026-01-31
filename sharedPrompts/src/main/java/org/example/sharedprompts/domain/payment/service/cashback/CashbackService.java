package org.example.sharedprompts.domain.payment.service.cashback;

import org.example.sharedprompts.dto.payment.response.CashbackResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * 캐시백 서비스 인터페이스
 */
public interface CashbackService {
    Page<CashbackResponseDto> getCashbackHistory(Long customerId, Pageable pageable);
    /**
     * 캐시백 적립
     */
    void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount);

    /**
     * 캐시백 지급
     */
    void payCashback(Long userId, Long cashbackId);

    Page<CashbackResponseDto> getUnpaidCashbacks(Long customerId, Pageable pageable);
    /**
     * 미지급 캐시백 총액 조회
     */
    BigDecimal getUnpaidCashbackTotal(Long userId);
}

