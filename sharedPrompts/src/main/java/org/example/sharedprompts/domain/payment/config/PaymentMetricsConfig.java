package org.example.sharedprompts.domain.payment.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.example.sharedprompts.domain.payment.metrics.PaymentMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 메트릭 설정
 */
@Configuration
public class PaymentMetricsConfig {

    /**
     * Register a bean that records payment-related metrics to the provided metrics registry.
     *
     * @param meterRegistry the MeterRegistry used by the created PaymentMetrics to record metrics
     * @return the PaymentMetrics instance configured to use the provided MeterRegistry
     */
    @Bean
    public PaymentMetrics paymentMetrics(MeterRegistry meterRegistry) {
        return new PaymentMetrics(meterRegistry);
    }
}
