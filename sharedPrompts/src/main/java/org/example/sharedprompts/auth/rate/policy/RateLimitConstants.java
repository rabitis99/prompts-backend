package org.example.sharedprompts.auth.rate.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Rate Limit 관련 상수 정의
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitConstants {

    /**
     * API 경로 상수
     */
    public static final class ApiPaths {
        public static final String API_PREFIX = "/api";
        public static final String AUTH_LOGIN = "/api/auth/login";
        public static final String AUTH_SIGNUP = "/api/auth/signup";
        public static final String PROMPTS = "/api/prompts";

        private ApiPaths() {
        }
    }

    /**
     * Rate Limit 규칙 이름 상수
     */
    public static final class RuleNames {
        public static final String LOGIN = "login";
        public static final String SIGNUP = "signup";
        public static final String PROMPT_CREATE = "prompt_create";
        public static final String GENERAL = "general";

        private RuleNames() {
        }
    }

    /**
     * Rate Limit 용량 상수
     * 
     * @deprecated 이 상수들은 더 이상 사용되지 않습니다.
     *             RateLimitProperties를 통해 application.yml에서 설정값을 로드하세요.
     *             운영 환경별로 한도 값을 조정할 수 있습니다.
     */
    @Deprecated
    public static final class Capacities {
        /**
         * @deprecated RateLimitProperties.getRules().getLogin()을 사용하세요.
         */
        @Deprecated
        public static final long LOGIN = 5L;
        
        /**
         * @deprecated RateLimitProperties.getRules().getSignup()을 사용하세요.
         */
        @Deprecated
        public static final long SIGNUP = 3L;
        
        /**
         * @deprecated RateLimitProperties.getRules().getPromptCreate()을 사용하세요.
         */
        @Deprecated
        public static final long PROMPT_CREATE = 10L;
        
        /**
         * @deprecated RateLimitProperties.getRules().getGeneral()을 사용하세요.
         */
        @Deprecated
        public static final long GENERAL = 100L;

        private Capacities() {
        }
    }

    /**
     * Rate Limit 윈도우 시간 상수 (초)
     * 
     * @deprecated 이 상수는 더 이상 사용되지 않습니다.
     *             RateLimitProperties.getWindows().getDefaultSeconds()를 사용하세요.
     */
    @Deprecated
    public static final class Windows {
        /**
         * @deprecated RateLimitProperties.getWindows().getDefaultSeconds()를 사용하세요.
         */
        @Deprecated
        public static final long DEFAULT_SECONDS = 60L;

        private Windows() {
        }
    }

    /**
     * Rate Limit 로깅 관련 상수
     */
    public static final class Logging {
        public static final String LOGGER_NAME = "rateLimit";
        public static final String LOG_FORMAT_IP = 
                "Rate limit exceeded - rule={}, key={}, currentCount={}, limit={}, retryAfter={}, ip={}, uri={}, method={}";
        public static final String LOG_FORMAT_USER = 
                "Rate limit exceeded - rule={}, key={}, currentCount={}, limit={}, retryAfter={}, userId={}, ip={}, uri={}, method={}";

        private Logging() {
        }
    }

    /**
     * Rate Limit 응답 관련 상수
     */
    public static final class Response {
        public static final long MIN_RETRY_AFTER_SECONDS = 1L;

        private Response() {
        }
    }
}

