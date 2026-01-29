package org.example.sharedprompts.domain.payment.service.cashback;

import java.math.BigDecimal;

/**
 * 캐시백 서비스 인터페이스
 */
public interface CashbackService {

    /**
     * 캐시백 적립
     */
    void accumulateCashback(Long userId, Long paymentId, BigDecimal paymentAmount);

    /**
     * 캐시백 지급
     */
    void payCashback(Long userId, Long cashbackId);

    /**
     * 미지급 캐시백 총액 조회
     */
    BigDecimal getUnpaidCashbackTotal(Long userId);
}

