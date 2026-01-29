package org.example.sharedprompts.domain.payment.service.exchange;

import java.math.BigDecimal;

/**
 * 환율 서비스 인터페이스
 */
public interface ExchangeRateService {

    /**
 * Converts an amount from one currency to another.
 *
 * @param amount       the monetary amount to convert
 * @param fromCurrency the ISO 4217 currency code of the source currency
 * @param toCurrency   the ISO 4217 currency code of the target currency
 * @return             the converted amount expressed in the target currency
 */
    BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
 * Retrieves the exchange rate to convert an amount from one currency to another.
 *
 * @param fromCurrency the three-letter ISO 4217 code of the source currency
 * @param toCurrency the three-letter ISO 4217 code of the target currency
 * @return the exchange rate as a BigDecimal representing the multiplier to convert an amount in {@code fromCurrency} to {@code toCurrency}
 */
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);
}
