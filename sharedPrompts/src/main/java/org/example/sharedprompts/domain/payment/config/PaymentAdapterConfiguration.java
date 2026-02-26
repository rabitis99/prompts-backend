package org.example.sharedprompts.domain.payment.config;

import org.springframework.context.annotation.Configuration;

/**
 * 결제 어댑터 빈 설정.
 * Repository / EventPublisher / Webhook / Gateway 어댑터는 각각 포트를 구현한 @Component로 등록되므로
 * 여기서 중복 @Bean을 두지 않습니다.
 */
@Configuration
public class PaymentAdapterConfiguration {
}
