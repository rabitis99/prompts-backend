package org.example.sharedprompts.domain.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 결제 도메인 재시도 설정
 * 
 * PaymentResultEventListener의 @Retryable 어노테이션 활성화
 */
@Configuration
@EnableRetry
public class PaymentRetryConfig {
}

