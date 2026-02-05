package org.example.sharedprompts.domain.payment.infrastructure.external.exchange;

import java.math.BigDecimal;

/**
 * 환율 서비스 인터페이스
 */
public interface ExchangeRateService {

    /**
     * 통화 변환
     */
    BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * 환율 조회
     */
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);
}

