package org.example.sharedprompts.auth.security.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.oauth.config.OAuth2Components;
import org.example.sharedprompts.auth.security.constant.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        // SecurityContextRepository 설정: HttpServletRequest 속성에 SecurityContext 저장
        // 비동기 응답 처리 시 SecurityContext를 유지하기 위해 필요
        http.securityContext(securityContext -> securityContext
                .securityContextRepository(new HttpServletRequestAttributeSecurityContextRepository()));

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

    /**
     * HttpServletRequest 속성에 SecurityContext를 저장하고 복원하는 SecurityContextRepository
     * 비동기 응답 처리 시 SecurityContext를 유지하기 위해 사용
     */
    private static class HttpServletRequestAttributeSecurityContextRepository implements SecurityContextRepository {
        /**
         * SecurityContextRepository 인터페이스 구현을 위한 메서드
         * 
         * <p>Spring Security 6+에서는 loadDeferredContext를 사용하므로 이 메서드는 실제로 호출되지 않습니다.
         * 인터페이스 호환성을 위해 유지되며, 내부적으로 loadDeferredContext를 호출합니다.
         * 
         * @deprecated Spring Security 6+에서는 loadDeferredContext를 사용합니다.
         * 이 메서드는 인터페이스 호환성을 위해 유지되며 실제로는 사용되지 않습니다.
         */
        @Deprecated
        @Override
        public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
            // Spring Security 6+에서는 loadDeferredContext를 사용하므로 이 메서드는 사용되지 않습니다.
            // 인터페이스 구현을 위해 loadDeferredContext를 호출하여 반환합니다.
            return loadDeferredContext(requestResponseHolder.getRequest()).get();
        }

        @Override
        public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
            return new DeferredSecurityContext() {
                private SecurityContext securityContext;
                private boolean isGenerated;

                @Override
                public SecurityContext get() {
                    if (this.securityContext == null) {
                        this.securityContext = (SecurityContext) request.getAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME);
                        if (this.securityContext == null) {
                            this.securityContext = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                            this.isGenerated = true;
                        }
                    }
                    return this.securityContext;
                }

                @Override
                public boolean isGenerated() {
                    get();
                    return this.isGenerated;
                }
            };
        }

        @Override
        public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
            if (context.getAuthentication() != null) {
                request.setAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME, context);
            } else {
                request.removeAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME);
            }
        }

        @Override
        public boolean containsContext(HttpServletRequest request) {
            return request.getAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME) != null;
        }
    }
}

