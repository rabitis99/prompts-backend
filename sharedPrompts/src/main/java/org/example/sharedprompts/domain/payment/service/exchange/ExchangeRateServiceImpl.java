package org.example.sharedprompts.domain.payment.service.exchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.ExchangeRate;
import org.example.sharedprompts.domain.payment.repository.exchange.ExchangeRateRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 환율 서비스 구현체
 * 
 * <p>환율 조회 및 통화 변환 수행
 * <ul>
 *   <li>캐싱: 환율은 실시간 정확도가 높을 필요 없으므로 캐싱 적용 (5분 TTL)</li>
 *   <li>Fallback: 역방향 환율 조회 시 역수 계산</li>
 *   <li>에러 처리: 환율 조회 실패 시 예외 발생</li>
 * </ul>
 * 
 * <p><strong>캐싱 전략:</strong>
 * - 캐시 키: "exchangeRate:{fromCurrency}:{toCurrency}"
 * - TTL: 5분 (application.yml에서 설정 가능)
 * - Stale-While-Revalidate: 캐시 만료 시 이전 값 사용 후 백그라운드 갱신 (향후 구현)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {

        if (fromCurrency == null || toCurrency == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "통화 코드는 필수입니다.");
        }

        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 환율 조회 (캐싱 적용)
     * 
     * <p>캐시 키: "exchangeRate:{fromCurrency}:{toCurrency}"
     * 캐시 만료 시간은 application.yml의 spring.cache.caffeine.spec에서 설정
     */
    @Override
    @Cacheable(value = "exchangeRate", key = "#fromCurrency + ':' + #toCurrency")
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        // 직접 환율 조회
        Optional<ExchangeRate> exchangeRateOpt = exchangeRateRepository.findByCurrencyPair(fromCurrency, toCurrency);
        if (exchangeRateOpt.isPresent()) {
            BigDecimal rate = exchangeRateOpt.get().getRate();
            log.debug("환율 조회 성공 (DB): from={}, to={}, rate={}", fromCurrency, toCurrency, rate);
            return rate;
        }

        // 역방향 환율 조회 (USD -> KRW가 없으면 KRW -> USD의 역수 사용)
        Optional<ExchangeRate> reverseRateOpt = exchangeRateRepository.findByCurrencyPair(toCurrency, fromCurrency);
        if (reverseRateOpt.isPresent()) {
            BigDecimal reverseRate = reverseRateOpt.get().getRate();
            BigDecimal calculatedRate = BigDecimal.ONE.divide(reverseRate, 6, RoundingMode.HALF_UP);
            log.debug("환율 조회 성공 (역수 계산): from={}, to={}, rate={}", fromCurrency, toCurrency, calculatedRate);
            return calculatedRate;
        }

        log.error("환율 정보를 찾을 수 없습니다: from={}, to={}", fromCurrency, toCurrency);
        throw new ApiException(ErrorCode.EXCHANGE_RATE_ERROR);
    }
}
