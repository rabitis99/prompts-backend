package org.example.sharedprompts.domain.payment.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 토스페이먼츠 설정 Properties
 */
@Getter
@Setter
@Component
@Validated
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "payment.toss")
@NoArgsConstructor
@AllArgsConstructor
public class TossPayProperties {

    /**
     * 토스페이먼츠 API 키 (필수)
     */
    @NotBlank(message = "토스페이먼츠 API 키는 필수입니다 (payment.toss.api-key)")
    private String apiKey;

    /**
     * 토스페이먼츠 시크릿 키 (필수)
     */
    @NotBlank(message = "토스페이먼츠 시크릿 키는 필수입니다 (payment.toss.secret-key)")
    private String secretKey;

    /**
     * 토스페이먼츠 API Base URL (기본값: https://api.tosspayments.com/v1/payments)
     */
    private String baseUrl = "https://api.tosspayments.com/v1/payments";

    /**
     * 결제 승인 엔드포인트 (기본값: /confirm)
     */
    private String confirmEndpoint = "/confirm";
}




