package org.example.sharedprompts.auth.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import static org.example.sharedprompts.auth.security.config.SecurityPathConstants.ADMIN_PATHS;
import static org.example.sharedprompts.auth.security.config.SecurityPathConstants.PUBLIC_PATHS;

/**
 * Spring Security 인가(Authorization) 규칙 설정
 *
 * <p>요청 경로별 접근 권한을 정의합니다.
 * RoleHierarchy가 자동으로 적용되어 ROLE_ADMIN은 ROLE_USER의 권한도 상속받습니다.
 *
 * <p>현재 규칙:
 * <ul>
 *   <li>공개 경로: 인증 없이 접근 가능 (회원가입, 로그인, OAuth2 콜백 등)</li>
 *   <li>기타 경로: 인증된 사용자만 접근 가능</li>
 * </ul>
 */
@Component
public class SecurityAuthorizationConfig {

    /**
     * 인가 규칙 설정
     *
     * <p>공개 경로:
     * <ul>
     *   <li>/auth/signup, /api/auth/signup: 회원가입</li>
     *   <li>/auth/login, /api/auth/login: 로그인</li>
     *   <li>/auth/callback, /api/auth/callback: OAuth2 콜백</li>
     *   <li>/auth/refresh, /api/auth/refresh: 토큰 갱신</li>
     *   <li>/oauth2/**, /login/**: OAuth2 로그인 관련</li>
     * </ul>
     *
     * @param http HttpSecurity 객체
     * @throws Exception 설정 중 오류 발생 시
     */
    public void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // 공개 경로 (인증 불필요)
                .requestMatchers(PUBLIC_PATHS).permitAll()
                
                // 관리자 전용 경로
                // RoleHierarchy에 의해 ROLE_ADMIN은 ROLE_USER의 권한도 상속받음
                .requestMatchers(ADMIN_PATHS).hasRole("ADMIN")
                
                // 기타 모든 경로는 인증 필요
                .anyRequest().authenticated()
        );
    }
}

