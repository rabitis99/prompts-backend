package org.example.sharedprompts.auth.rate.policy;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Rate Limit 설정 Properties
 *
 * application.yml에서 Rate Limit 관련 설정을 로드합니다.
 * 운영 환경별로 한도/윈도우 값을 조정할 수 있습니다.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "rate-limit")
@Validated
public class RateLimitProperties {

    /**
     * Rate Limit 규칙별 한도 설정
     */
    @Valid
    private RateLimitRules rules = new RateLimitRules();

    /**
     * Rate Limit 윈도우 설정
     */
    @Valid
    private RateLimitWindows windows = new RateLimitWindows();

    /**
     * Rate Limit 실패 정책 설정
     */
    @Valid
    private RateLimitFailurePolicy failurePolicy = new RateLimitFailurePolicy();
}
