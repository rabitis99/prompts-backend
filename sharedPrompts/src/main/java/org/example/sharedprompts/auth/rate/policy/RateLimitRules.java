package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Min;

/**
 * Rate Limit 규칙별 한도 설정
 */
@Setter
@Getter
public class RateLimitRules {
    /**
     * 로그인 한도
     */
    @Min(1)
    private long login = 5L;

    /**
     * 회원가입 한도
     */
    @Min(1)
    private long signup = 3L;

    /**
     * OAuth2 로그인 확정 한도
     */
    @Min(1)
    private long confirm = 5L;

    /**
     * 프롬프트 생성 한도
     */
    @Min(1)
    private long promptCreate = 10L;

    /**
     * 일반 API 한도
     */
    @Min(1)
    private long general = 100L;
}

