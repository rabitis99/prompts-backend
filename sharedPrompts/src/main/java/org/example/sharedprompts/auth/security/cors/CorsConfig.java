package org.example.sharedprompts.auth.security.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.example.sharedprompts.auth.security.cors.CorsConstants.ADMIN_PATH_PATTERN;
import static org.example.sharedprompts.auth.security.cors.CorsConstants.DEFAULT_PATH_PATTERN;

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

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Spring Security에서 사용할 CORS 설정 소스
     *
     * <p>경로별로 다른 CORS 정책을 적용합니다:
     * <ul>
     *   <li>/admin/**: 관리자 전용 CORS 정책</li>
     *   <li>기타 경로: 기본 CORS 정책</li>
     * </ul>
     *
     * <p>주의: Spring Security의 필터 체인에서 CORS가 처리될 때,
     * context-path(/api)는 이미 제거된 상태로 경로 매칭이 수행됩니다.
     * 따라서 /admin/** 패턴을 사용해야 SecurityPathConstants.ADMIN_PATHS와 일치합니다.
     *
     * <p>Preflight(OPTIONS) 요청도 포함하여 처리합니다.
     *
     * @return CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Origin 파싱 및 검증 (와일드카드 체크 포함)
        List<String> origins = CorsOriginValidator.parseAndValidate(corsAllowedOrigins);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 관리자 API용 CORS 정책
        // context-path(/api)가 제거된 상태이므로 /admin/** 패턴 사용
        source.registerCorsConfiguration(
                ADMIN_PATH_PATTERN,
                CorsConfigurationFactory.createAdmin(origins)
        );

        // 기본 CORS 정책 (나머지 모든 경로)
        source.registerCorsConfiguration(
                DEFAULT_PATH_PATTERN,
                CorsConfigurationFactory.createDefault(origins)
        );

        return source;
    }
}


