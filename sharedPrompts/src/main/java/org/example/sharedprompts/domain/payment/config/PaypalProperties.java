package org.example.sharedprompts.domain.payment.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * PayPal 설정 Properties
 *
 * <p><strong>검증 전략:</strong>
 * - @ConfigurationProperties: 설정값을 자동으로 바인딩
 * - @ConditionalOnProperty: payment.enabled=true일 때만 빈 로드 (배포 안전성)
 * - @Validated: 부팅 시점에 필수값 검증 (fail-fast)
 * - @NotBlank: returnUrl과 cancelUrl는 필수
 *
 * <p>payment.enabled=false면 이 빈이 로드되지 않아 환경변수 없이도 부팅 가능.
 * payment.enabled=true이고 설정값 누락 시 BindException 발생하여 애플리케이션 부팅 실패
 * (런타임이 아닌 부팅 시점에 오류 감지)
 */
@Getter
@Setter
@Component
@Validated
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "payment.paypal")
public class PaypalProperties {

    private String clientId;
    private String clientSecret;
    private String webhookId;
    private String baseUrl = "https://api-m.paypal.com"; // 기본값: 프로덕션 URL

    @NotBlank(message = "PayPal return URL은 필수입니다 (payment.paypal.return-url)")
    private String returnUrl;

    @NotBlank(message = "PayPal cancel URL은 필수입니다 (payment.paypal.cancel-url)")
    private String cancelUrl;
}




