package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
public class RateLimitProperties {

    /**
     * Rate Limit 규칙별 한도 설정
     */
    private Rules rules = new Rules();

    /**
     * Rate Limit 윈도우 설정
     */
    private Windows windows = new Windows();

    /**
     * Rate Limit 규칙별 한도
     */
    @Setter
    @Getter
    public static class Rules {
        /**
         * 로그인 한도
         */
        private long login = 5L;

        /**
         * 회원가입 한도
         */
        private long signup = 3L;

        /**
         * 프롬프트 생성 한도
         */
        private long promptCreate = 10L;

        /**
         * 일반 API 한도
         */
        private long general = 100L;
    }

    /**
     * Rate Limit 윈도우 설정
     */
    @Setter
    @Getter
    public static class Windows {
        /**
         * 기본 윈도우 시간 (초)
         */
        private long defaultSeconds = 60L;
    }
}

