package org.example.sharedprompts.domain.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 결제 도메인 재시도 설정
 * 
 * PaymentResultEventListener의 @Retryable 어노테이션 활성화
 * 
 * order = HIGHEST_PRECEDENCE로 설정하여 retry interceptor가 transaction interceptor 외부에 위치하도록 함.
 * 이를 통해 각 재시도마다 새로운 트랜잭션이 시작되어 OptimisticLockingFailureException 발생 시
 * EntityManager의 상태가 오염되지 않도록 보장합니다.
 */
@Configuration
@EnableRetry(order = Ordered.HIGHEST_PRECEDENCE)
public class PaymentRetryConfig {
}

