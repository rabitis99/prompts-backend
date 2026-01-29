package org.example.sharedprompts.domain.payment.service.exchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.ExchangeRate;
import org.example.sharedprompts.domain.payment.repository.ExchangeRateRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 환율 서비스 구현체
 * 엔티티에서 환율을 조회하여 통화 변환 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    /**
     * Converts a monetary amount from one currency to another using the applicable exchange rate.
     *
     * <p>If the source and target currencies are the same, the original amount is returned. Otherwise
     * the amount is multiplied by the resolved exchange rate and rounded to 2 decimal places using
     * HALF_UP rounding.
     *
     * @param amount       the monetary amount to convert
     * @param fromCurrency the source currency code
     * @param toCurrency   the target currency code
     * @return the converted amount rounded to 2 decimal places (HALF_UP)
     */
    @Override
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Resolves the exchange rate to convert an amount from one currency to another.
     *
     * <p>If a direct rate exists for the pair, it is returned. If a direct rate does not exist but
     * the reverse pair exists, the reciprocal of the reverse rate is returned with scale 6 and
     * HALF_UP rounding. If the currencies are identical, `BigDecimal.ONE` is returned.</p>
     *
     * @param fromCurrency the source currency code (e.g., "USD")
     * @param toCurrency   the target currency code (e.g., "KRW")
     * @return the exchange rate to multiply an amount in {@code fromCurrency} to obtain the amount in {@code toCurrency}
     * @throws ApiException when no exchange rate information is available for the currency pair
     */
    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // 직접 환율 조회
        Optional<ExchangeRate> exchangeRateOpt = exchangeRateRepository.findByCurrencyPair(fromCurrency, toCurrency);
        if (exchangeRateOpt.isPresent()) {
            return exchangeRateOpt.get().getRate();
        }

        // 역방향 환율 조회 (USD -> KRW가 없으면 KRW -> USD의 역수 사용)
        Optional<ExchangeRate> reverseRateOpt = exchangeRateRepository.findByCurrencyPair(toCurrency, fromCurrency);
        if (reverseRateOpt.isPresent()) {
            BigDecimal reverseRate = reverseRateOpt.get().getRate();
            return BigDecimal.ONE.divide(reverseRate, 6, RoundingMode.HALF_UP);
        }

        log.error("환율 정보를 찾을 수 없습니다: from={}, to={}", fromCurrency, toCurrency);
        throw new ApiException(ErrorCode.EXCHANGE_RATE_ERROR);
    }
}