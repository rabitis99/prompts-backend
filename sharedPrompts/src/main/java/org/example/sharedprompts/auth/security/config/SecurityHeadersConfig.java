package org.example.sharedprompts.auth.security.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * Spring Security 보안 헤더 설정
 *
 * <p>보안 헤더를 설정하여 다양한 보안 공격을 방어합니다:
 * <ul>
 *   <li>X-Frame-Options: 클릭재킹 공격 방어</li>
 *   <li>X-Content-Type-Options: MIME 타입 스니핑 방어</li>
 *   <li>Strict-Transport-Security (HSTS): HTTPS 강제</li>
 *   <li>Content-Security-Policy: XSS 공격 방어</li>
 * </ul>
 */
@Component
public class SecurityHeadersConfig {

    /**
     * 보안 헤더 설정
     *
     * @param http HttpSecurity 객체
     * @throws Exception 설정 중 오류 발생 시
     */
    public void configureHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                // X-Frame-Options: iframe 임베딩 차단 (클릭재킹 방어)
                .frameOptions(frame -> frame.deny())
                
                // X-Content-Type-Options: MIME 타입 스니핑 방어
                .contentTypeOptions(Customizer.withDefaults())
                
                // Strict-Transport-Security: HTTPS 강제 (1년)
                .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000) // 1년
                        .includeSubDomains(true))
                
                // Content-Security-Policy: XSS 공격 방어
                .contentSecurityPolicy(csp ->
                        csp.policyDirectives("default-src 'self'"))
        );
    }
}

