package org.example.sharedprompts.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정
 *
 * Spring Security를 사용하는 경우 CORS는 Security Filter Chain에서만 처리됩니다.
 * WebMvcConfigurer 기반 CORS 설정은 제거되었으며,
 * 모든 CORS 정책은 Security Filter Chain에서 단일 관리됩니다.
 *
 * - Single Source of Truth
 * - allowCredentials(true) + allowedOriginPatterns 사용
 */
@Configuration
public class WebMvcConfig {

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Content-Type", "Authorization", "Accept", "Origin");

    private static final long MAX_AGE = 3600L;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Origin 설정 파싱 및 검증
     *
     * allowCredentials(true) 사용 시
     * "*" 단독 사용은 금지한다.
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
     * Spring Security에서 사용할 CORS 설정
     *
     * - Preflight(OPTIONS) 요청 포함
     * - Security Filter Chain 기준 단일 CORS 정책
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = parseAndValidateOrigins();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(MAX_AGE);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
