package org.example.sharedprompts.scheduler.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.sharedprompts.domain.payment.ExchangeRate;
import org.example.sharedprompts.domain.payment.config.ExchangeRateProperties;
import org.example.sharedprompts.domain.payment.repository.exchange.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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
 * - 앱 시작 시 EventListener 기반 초기화
 * - 트랜잭션 적용
 * - 책임 분리로 가독성 향상
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateProperties exchangeRateProperties;
    @Qualifier("paymentRestTemplate")
    private final RestTemplate restTemplate;

    private static final List<String> SUPPORTED_CURRENCIES = List.of("KRW", "EUR", "JPY", "CNY", "GBP");

    /**
     * 애플리케이션 시작 시 환율 초기 로드
     * 멀티 인스턴스 환경에서 중복 실행 방지를 위한 분산 락 적용
     */
    @EventListener(ContextRefreshedEvent.class)
    @SchedulerLock(
            name = "ExchangeRateScheduler",
            lockAtMostFor = "30m",
            lockAtLeastFor = "5m"
    )
    @LockProviderToUse("fallbackLockProvider")
    @Transactional
    public void initializeExchangeRates() {
        log.info("애플리케이션 시작 시 환율 초기 로드 시작");
        try {
            doUpdateExchangeRates();
        } catch (Exception e) {
            log.warn("환율 초기 로드 실패 (스케줄러에서 재시도됨): {}", e.getMessage());
        }
    }

    /**
     * 환율 업데이트 (기본 1시간 간격)
     * 
     * <p><strong>스케줄러 기반 환율 정책:</strong>
     * - 외부 환율 API에 대한 실시간 의존도를 줄이기 위해 스케줄러로 주기적 갱신
     * - 기본값: 1시간 간격 (application.yml에서 설정 가능)
     * - 장점: 외부 API 장애 시에도 최근 갱신된 환율로 결제 진행 가능
     * 
     * <p><strong>설정 방법:</strong>
     * application.yml에서 `payment.exchange-rate.schedule` 속성으로 cron 표현식 설정
     * 예: `0 0 * * * ?` (매 시간 정각), `0 0 0/1 * * ?` (1시간 간격)
     */
    @Scheduled(cron = "${payment.exchange-rate.schedule:0 0 * * * ?}")
    @SchedulerLock(
            name = "ExchangeRateScheduler",
            lockAtMostFor = "30m",
            lockAtLeastFor = "5m"
    )
    @LockProviderToUse("fallbackLockProvider")
    @Transactional
    public void updateExchangeRates() {
        doUpdateExchangeRates();
    }

    /**
     * 실제 환율 업데이트 로직 (책임 분리)
     */
    private void doUpdateExchangeRates() {
        log.info("ExchangeRateScheduler 실행 시작");

        Map<String, Object> rates = fetchRatesFromApi();
        if (rates == null) return;

        LocalDateTime fetchedAt = LocalDateTime.now();
        int updatedCount = 0;
        int createdCount = 0;

        for (String currency : SUPPORTED_CURRENCIES) {
            Object rateObj = rates.get(currency);
            if (rateObj == null) continue;

            BigDecimal rate = convertToBigDecimal(rateObj);
            boolean updated = upsertExchangeRate("USD", currency, rate, fetchedAt);

            if (updated) updatedCount++;
            else createdCount++;
        }

        log.info("ExchangeRateScheduler 실행 완료: updated={}, created={}", updatedCount, createdCount);
    }

    /**
     * 1️⃣ 환율 API 호출
     */
    private Map<String, Object> fetchRatesFromApi() {
        String apiUrl = exchangeRateProperties.getApiUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            log.warn("환율 API URL이 설정되지 않았습니다.");
            return null;
        }

        String baseCurrency = "USD";
        String url = apiUrl + baseCurrency;

        HttpHeaders headers = new HttpHeaders();
        if (exchangeRateProperties.getApiKey() != null && !exchangeRateProperties.getApiKey().isEmpty()) {
            headers.set("apikey", exchangeRateProperties.getApiKey());
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, new ParameterizedTypeReference<>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("환율 API 호출 실패: status={}", response.getStatusCode());
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.getBody().get("rates");
            if (rates == null) {
                log.warn("환율 API 응답에 rates 필드가 없습니다.");
                return null;
            }

            return rates;

        } catch (Exception e) {
            log.error("환율 API 호출 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 2️⃣ DB 업데이트/생성
     * @return true = 업데이트, false = 생성
     */
    private boolean upsertExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDateTime fetchedAt) {
        ExchangeRate exchangeRate = exchangeRateRepository
                .findByCurrencyPair(fromCurrency, toCurrency)
                .orElse(null);

        if (exchangeRate != null) {
            exchangeRate.updateRate(rate, fetchedAt);
            exchangeRateRepository.save(exchangeRate);
            return true;
        } else {
            exchangeRate = ExchangeRate.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(rate)
                    .lastFetchedAt(fetchedAt)
                    .build();
            exchangeRateRepository.save(exchangeRate);
            return false;
        }
    }

    /**
     * 3️⃣ Object -> BigDecimal 변환
     * double 경유를 피하고 toString() 기반 변환으로 정밀도 손실 방지
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        if (value instanceof String) return new BigDecimal((String) value);
        throw new IllegalArgumentException("환율 값을 BigDecimal로 변환할 수 없습니다: " + value);
    }
}
