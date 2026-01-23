package org.example.sharedprompts.auth.security;

import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.example.sharedprompts.auth.security.CorsConstants.*;

/**
 * CORS 설정 팩토리
 * 
 * 다양한 CORS 정책에 맞는 CorsConfiguration을 생성합니다.
 */
public final class CorsConfigurationFactory {

    private CorsConfigurationFactory() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 기본 CORS 설정 생성
     *
     * <p>일반 API에 적용되는 기본 CORS 정책입니다.
     * - 모든 허용된 메서드 사용
     * - 모든 허용된 헤더 사용
     * - Credentials 허용
     * - Preflight 캐시 시간: 1시간
     *
     * @param origins 허용할 origin 목록
     * @return CORS 설정
     */
    public static CorsConfiguration createDefault(List<String> origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(MAX_AGE_SECONDS);
        return configuration;
    }

    /**
     * 관리자 API용 CORS 설정 생성
     *
     * <p>관리자 API에 적용되는 CORS 정책입니다.
     * 현재는 기본 설정과 동일하지만, 향후 더 엄격한 정책을 적용할 수 있습니다.
     *
     * <p>향후 확장 예시:
     * <ul>
     *   <li>특정 메서드만 허용: GET, POST, PUT, DELETE</li>
     *   <li>더 제한적인 origin 목록 사용</li>
     *   <li>더 짧은 Preflight 캐시 시간</li>
     * </ul>
     *
     * @param origins 허용할 origin 목록
     * @return CORS 설정
     */
    public static CorsConfiguration createAdmin(List<String> origins) {
        // 현재는 기본 설정과 동일
        // 향후 관리자 API에 더 엄격한 정책을 적용하려면:
        // CorsConfiguration config = createDefault(origins);
        // config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        // return config;
        return createDefault(origins);
    }
}

