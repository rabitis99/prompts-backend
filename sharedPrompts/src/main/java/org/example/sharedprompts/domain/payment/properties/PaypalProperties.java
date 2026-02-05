package org.example.sharedprompts.domain.payment.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * PayPal 설정 Properties
 */
@Getter
@Setter
@Component
@Validated
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "payment.paypal")
public class PaypalProperties {
    @NotBlank(message = "PayPal clientId는 필수입니다 (payment.paypal.client-id)")
    private String clientId;

    @NotBlank(message = "PayPal clientSecret은 필수입니다 (payment.paypal.client-secret)")
    private String clientSecret;

    @NotBlank(message = "PayPal webhookId는 필수입니다 (payment.paypal.webhook-id)")
    private String webhookId;

    private String baseUrl = "https://api-m.paypal.com"; // 기본값: 프로덕션 URL

    @NotBlank(message = "PayPal return URL은 필수입니다 (payment.paypal.return-url)")
    private String returnUrl;

    @NotBlank(message = "PayPal cancel URL은 필수입니다 (payment.paypal.cancel-url)")
    private String cancelUrl;
}