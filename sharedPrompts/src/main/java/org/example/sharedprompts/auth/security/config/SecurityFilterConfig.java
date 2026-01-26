package org.example.sharedprompts.auth.security.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.jwt.filter.JwtAuthenticationFilter;
import org.example.sharedprompts.auth.rate.filter.impl.IpRateLimitFilter;
import org.example.sharedprompts.auth.rate.filter.impl.UserRateLimitFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * Spring Security Filter Chain 설정
 *
 * <p>Security Filter Chain의 Filter 순서를 설정합니다.
 *
 * <p>Filter 실행 순서 (위에서 아래로):
 * <ol>
 *   <li>IpRateLimitFilter: JWT 인증 전에 IP 기반 Rate Limit 체크 (인증되지 않은 사용자도 제한)</li>
 *   <li>JwtAuthenticationFilter: JWT 토큰 검증 및 인증 처리</li>
 *   <li>UserRateLimitFilter: JWT 인증 후 사용자 ID 기반 Rate Limit 체크 (인증된 사용자만 제한)</li>
 * </ol>
 *
 * <p>이 순서는 다음과 같은 이유로 중요합니다:
 * <ul>
 *   <li>IP 기반 Rate Limit은 인증 전에 실행되어 무차별 대입 공격을 방지합니다.</li>
 *   <li>JWT 인증은 사용자 식별을 위해 필수적입니다.</li>
 *   <li>사용자 기반 Rate Limit은 인증 후에만 실행되어 정확한 사용자 식별이 가능합니다.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SecurityFilterConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final IpRateLimitFilter ipRateLimitFilter;
    private final UserRateLimitFilter userRateLimitFilter;

    /**
     * Filter 순서 설정
     *
     * @param http HttpSecurity 객체
     */
    public void configure(HttpSecurity http) {
        // AsyncSecurityContextRestoreFilter는 @Order(-300)으로 설정되어 자동으로 가장 먼저 실행됨
        
        // 2. JWT 인증 필터를 먼저 추가 (표준 필터를 기준으로)
        //    @Order(-100)로 설정되어 있어 다른 커스텀 필터보다 나중에 실행됨
        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );
        
        // 1. IP 기반 Rate Limit (JWT 인증 전에 실행되도록)
        //    @Order(-200)로 설정되어 JwtAuthenticationFilter보다 먼저 실행됨
        //    AsyncSecurityContextRestoreFilter(@Order(-300)) 다음으로 실행됨
        http.addFilterBefore(
                ipRateLimitFilter,
                JwtAuthenticationFilter.class
        );
        
        // 3. 사용자 기반 Rate Limit (JWT 인증 후에 실행되도록)
        //    @Order(-50)로 설정되어 있지만, addFilterAfter로 명시적으로 JWT 필터 뒤에 배치
        http.addFilterAfter(
                userRateLimitFilter,
                JwtAuthenticationFilter.class
        );
    }
}


