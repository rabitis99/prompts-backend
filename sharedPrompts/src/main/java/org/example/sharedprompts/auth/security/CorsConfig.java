package org.example.sharedprompts.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security용 CORS 설정
 *
 * <p>경로별로 다른 CORS 정책을 적용할 수 있습니다.
 * Spring Security를 사용하는 경우 CORS는 Security Filter Chain에서만 처리됩니다.
 *
 * <p>현재 구조:
 * <ul>
 *   <li>일반 API: 기본 CORS 정책 적용</li>
 *   <li>관리자 API (/api/admin/**): 관리자 전용 CORS 정책 (필요시 더 엄격한 정책 적용 가능)</li>
 * </ul>
 *
 * <p>주의사항:
 * <ul>
 *   <li>allowCredentials(true) 사용 시 "*" 단독 origin 사용 불가</li>
 *   <li>allowedOriginPatterns를 사용하여 패턴 기반 origin 허용</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Content-Type", "Authorization", "Accept", "Origin", "X-Requested-With");

    private static final long MAX_AGE = 3600L;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Origin 설정 파싱 및 검증
     *
     * <p>allowCredentials(true) 사용 시 "*" 단독 사용은 금지됩니다.
     *
     * @return 검증된 origin 목록
     * @throws IllegalStateException "*" 단독 사용 시
     */
    private List<String> parseAndValidateOrigins() {
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        boolean isWildcardOnly =
                origins.size() == 1 && "*".equals(origins.get(0));

        if (isWildcardOnly) {
            throw new IllegalStateException(
                    "CORS 설정 오류: allowCredentials(true)에서는 '*' origin을 사용할 수 없습니다."
            );
        }

        return origins;
    }

    /**
     * 기본 CORS 설정 생성
     *
     * <p>일반 API에 적용되는 기본 CORS 정책입니다.
     *
     * @param origins 허용할 origin 목록
     * @return CORS 설정
     */
    private CorsConfiguration createDefaultCorsConfiguration(List<String> origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(MAX_AGE);
        return configuration;
    }

    /**
     * 관리자 API용 CORS 설정 생성
     *
     * <p>관리자 API에 적용되는 CORS 정책입니다.
     * 필요시 더 엄격한 정책을 적용할 수 있습니다.
     *
     * @param origins 허용할 origin 목록
     * @return CORS 설정
     */
    private CorsConfiguration createAdminCorsConfiguration(List<String> origins) {
        // 현재는 기본 설정과 동일하지만, 향후 더 엄격한 정책 적용 가능
        CorsConfiguration configuration = createDefaultCorsConfiguration(origins);
        
        // 예시: 관리자 API는 특정 메서드만 허용
        // configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        
        return configuration;
    }

    /**
     * Spring Security에서 사용할 CORS 설정 소스
     *
     * <p>경로별로 다른 CORS 정책을 적용합니다:
     * <ul>
     *   <li>/api/admin/**: 관리자 전용 CORS 정책</li>
     *   <li>기타 경로: 기본 CORS 정책</li>
     * </ul>
     *
     * <p>Preflight(OPTIONS) 요청도 포함하여 처리합니다.
     *
     * @return CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = parseAndValidateOrigins();

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // 관리자 API용 CORS 정책
        CorsConfiguration adminConfig = createAdminCorsConfiguration(origins);
        source.registerCorsConfiguration("/api/admin/**", adminConfig);

        // 기본 CORS 정책 (나머지 모든 경로)
        CorsConfiguration defaultConfig = createDefaultCorsConfiguration(origins);
        source.registerCorsConfiguration("/**", defaultConfig);

        return source;
    }
}

