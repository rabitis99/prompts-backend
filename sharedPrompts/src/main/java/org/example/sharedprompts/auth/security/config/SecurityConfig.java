package org.example.sharedprompts.auth.security.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.oauth.config.OAuth2Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2Components oAuth2Components;
    private final CorsConfigurationSource corsConfigurationSource;
    private final SecurityHeadersConfig securityHeadersConfig;
    private final SecurityAuthorizationConfig securityAuthorizationConfig;
    private final SecurityExceptionHandlers securityExceptionHandlers;
    private final SecurityFilterConfig securityFilterConfig;

    /* =========================
       Authentication Manager
       ========================= */

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /* =========================
       Security Filter Chain
       ========================= */

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 기본 설정
        http
                // CSRF, Session 사용 안 함 (JWT 기반 인증)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 보안 헤더 설정
        securityHeadersConfig.configureHeaders(http);

        // CORS 설정 (경로별 세분화된 정책 적용)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        // 인가 규칙 설정
        // RoleHierarchy가 자동으로 적용되어 ROLE_ADMIN은 ROLE_USER의 권한도 상속받습니다.
        securityAuthorizationConfig.configureAuthorization(http);

        // 인증 / 인가 예외 처리
        securityExceptionHandlers.configure(http);

        // OAuth2 로그인
        oAuth2Components.configureOAuth2Login(http);

        // Rate Limiting 필터 + JWT 인증 필터
        securityFilterConfig.configure(http);

        return http.build();
    }
}

