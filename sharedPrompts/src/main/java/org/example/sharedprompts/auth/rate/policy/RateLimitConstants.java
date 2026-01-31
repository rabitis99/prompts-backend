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
        public static final String AUTH_CONFIRM = "/api/auth/confirm";
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
        public static final String CONFIRM = "confirm";
        public static final String PROMPT_CREATE = "prompt_create";
        public static final String GENERAL = "general";

        private RuleNames() {
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

