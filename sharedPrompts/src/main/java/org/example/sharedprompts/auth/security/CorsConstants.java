package org.example.sharedprompts.auth.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CORS 설정 관련 상수
 * 
 * CORS 정책에 사용되는 상수 값들을 정의합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CorsConstants {

    /**
     * 허용할 HTTP 메서드 목록
     */
    public static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * 허용할 HTTP 헤더 목록
     */
    public static final List<String> ALLOWED_HEADERS =
            List.of("Content-Type", "Authorization", "Accept", "Origin", "X-Requested-With");

    /**
     * Preflight 요청 캐시 시간 (초)
     */
    public static final long MAX_AGE_SECONDS = 3600L;

    /**
     * 와일드카드 origin 패턴
     */
    public static final String WILDCARD_ORIGIN = "*";

    /**
     * 관리자 API 경로 패턴
     * Spring Security 필터 체인에서 context-path(/api)가 제거된 상태이므로 /admin/** 사용
     */
    public static final String ADMIN_PATH_PATTERN = "/admin/**";

    /**
     * 기본 API 경로 패턴 (모든 경로)
     */
    public static final String DEFAULT_PATH_PATTERN = "/**";

    /**
     * Origin 구분자 (CSV 형식)
     */
    public static final String ORIGIN_SEPARATOR = ",";

    /**
     * CORS 설정 오류 메시지
     */
    public static final class ErrorMessages {
        private ErrorMessages() {
            // 상수 클래스이므로 인스턴스화 방지
        }

        public static final String ALLOWED_ORIGINS_NOT_SET = 
                "CORS 설정 오류: allowed-origins가 설정되지 않았습니다.";

        public static final String WILDCARD_NOT_ALLOWED_WITH_CREDENTIALS = 
                "CORS 설정 오류: allowCredentials(true)에서는 '*' origin을 사용할 수 없습니다. " +
                "현재 설정: %s";
    }
}

