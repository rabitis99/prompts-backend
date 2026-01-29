package org.example.sharedprompts.scheduler.payment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.payment.ExchangeRate;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.example.sharedprompts.domain.payment.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 환율 업데이트 스케줄러
 * 주기적으로 환율 API를 호출하여 환율 정보를 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateRepository exchangeRateRepository;
    private final PaymentProperties paymentProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;

    // 지원하는 주요 통화 목록 (기준 통화: USD)
    private static final List<String> SUPPORTED_CURRENCIES = List.of("KRW", "EUR", "JPY", "CNY", "GBP");

    /**
         * Performs a one-time exchange-rate load when the application starts.
         *
         * <p>This method is invoked once after bean construction. The initial load runs without a
         * transaction or distributed lock; failures are caught and logged so scheduled retries can
         * handle subsequent attempts.</p>
         */
    @PostConstruct
    public void initializeExchangeRates() {
        log.info("애플리케이션 시작 시 환율 초기 로드 시작");
        try {
            // 초기 로드 시에는 락 없이 실행 (애플리케이션 시작 단계이므로 중복 실행 걱정 없음)
            updateExchangeRates();
        } catch (Exception e) {
            log.warn("환율 초기 로드 실패 (스케줄러에서 재시도됨): {}", e.getMessage());
        }
    }

    /**
     * Fetches exchange rates from the configured external API and updates or creates local ExchangeRate records.
     *
     * Reads the exchange rate API URL and optional API key from PaymentProperties, requests rates using USD as the base currency, and for each supported target currency updates an existing ExchangeRate's rate and lastFetchedAt or creates a new ExchangeRate. If the API URL is missing or the response does not contain rates, a warning is logged; other failures are logged as errors.
     */
    @Scheduled(cron = "${payment.exchange-rate.schedule:0 0 2 * * ?}")
    @SchedulerLock(
            name = "ExchangeRateScheduler",
            lockAtMostFor = "30m",
            lockAtLeastFor = "5m"
    )
    @LockProviderToUse("fallbackLockProvider")
    @Transactional
    public void updateExchangeRates() {
        log.info("ExchangeRateScheduler started");

        try {
            String apiUrl = paymentProperties.getExchangeRateApiUrl();
            if (apiUrl == null || apiUrl.isEmpty()) {
                log.warn("환율 API URL이 설정되지 않았습니다.");
                return;
            }

            // 기준 통화를 USD로 설정
            String baseCurrency = "USD";
            String url = apiUrl + baseCurrency;

            HttpHeaders headers = new HttpHeaders();
            if (paymentProperties.getExchangeRateApiKey() != null && !paymentProperties.getExchangeRateApiKey().isEmpty()) {
                headers.set("apikey", paymentProperties.getExchangeRateApiKey());
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) responseBody.get("rates");
                
                if (rates != null) {
                    LocalDateTime fetchedAt = LocalDateTime.now();
                    int updatedCount = 0;
                    int createdCount = 0;

                    for (String currency : SUPPORTED_CURRENCIES) {
                        Object rateObj = rates.get(currency);
                        if (rateObj != null) {
                            BigDecimal rate = convertToBigDecimal(rateObj);
                            
                            // 환율 업데이트 또는 생성
                            ExchangeRate exchangeRate = exchangeRateRepository
                                    .findByCurrencyPair(baseCurrency, currency)
                                    .orElse(null);

                            if (exchangeRate != null) {
                                exchangeRate.updateRate(rate, fetchedAt);
                                exchangeRateRepository.save(exchangeRate);
                                updatedCount++;
                            } else {
                                exchangeRate = ExchangeRate.builder()
                                        .fromCurrency(baseCurrency)
                                        .toCurrency(currency)
                                        .rate(rate)
                                        .lastFetchedAt(fetchedAt)
                                        .build();
                                exchangeRateRepository.save(exchangeRate);
                                createdCount++;
                            }
                        }
                    }

                    log.info("ExchangeRateScheduler finished: updated={}, created={}", updatedCount, createdCount);
                } else {
                    log.warn("환율 API 응답에 rates 필드가 없습니다.");
                }
            } else {
                log.error("환율 API 호출 실패: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("ExchangeRateScheduler 실행 중 오류 발생", e);
        }
    }

    /**
     * Convert a numeric or numeric-string value to a BigDecimal.
     *
     * @param value a Number or a String containing a numeric representation
     * @return a BigDecimal representing the numeric value
     * @throws IllegalArgumentException if the value is neither a Number nor a String
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("환율 값을 BigDecimal로 변환할 수 없습니다: " + value);
    }
}
