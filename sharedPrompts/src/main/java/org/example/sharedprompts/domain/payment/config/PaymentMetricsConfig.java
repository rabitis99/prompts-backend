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

    @Bean
    public PaymentMetrics paymentMetrics(MeterRegistry meterRegistry) {
        return new PaymentMetrics(meterRegistry);
    }
}

