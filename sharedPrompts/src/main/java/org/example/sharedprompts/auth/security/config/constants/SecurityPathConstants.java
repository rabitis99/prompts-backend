package org.example.sharedprompts.auth.security.config.constants;

/**
 * Spring Security 인가 규칙에서 사용하는 경로 상수
 *
 * <p>인가 규칙에서 사용하는 경로를 상수로 관리하여:
 * <ul>
 *   <li>경로 변경 시 한 곳에서만 수정</li>
 *   <li>타이핑 오류 방지</li>
 *   <li>재사용성 향상</li>
 * </ul>
 *
 * <p><strong>중요:</strong> Spring Security의 {@code requestMatchers()}는
 * {@code server.servlet.context-path}가 제거된 servlet path 기준으로 동작합니다.
 * 따라서 context-path({@code /api})를 포함한 경로는 매칭되지 않으므로
 * servlet path만 사용해야 합니다.
 */
public final class SecurityPathConstants {

    private SecurityPathConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 공개 경로 (인증 불필요)
     *
     * <p>다음 경로들은 인증 없이 접근 가능합니다:
     * <ul>
     *   <li>회원가입: /auth/signup</li>
     *   <li>로그인: /auth/login</li>
     *   <li>OAuth2 콜백: /auth/callback</li>
     *   <li>토큰 갱신: /auth/refresh</li>
     *   <li>OAuth2 로그인: /oauth2/**, /login/**</li>
     *   <li>Actuator Health: /actuator/health (헬스 체크용)</li>
     * </ul>
     *
     * <p><strong>주의:</strong> context-path({@code /api})는 포함하지 않습니다.
     * 실제 요청 경로는 {@code /api/auth/signup}이지만,
     * Spring Security는 servlet path인 {@code /auth/signup}을 기준으로 매칭합니다.
     */
    public static final String[] PUBLIC_PATHS = {
            "/auth/signup",
            "/auth/login",
            "/auth/callback",
            "/auth/refresh",
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/callback",
            "/api/auth/refresh",
            "/oauth2/**",
            "/login/**",
            "/actuator/health",  // 헬스 체크는 공개 접근 허용
    };

    /**
     * 관리자 전용 경로
     *
     * <p>다음 경로들은 ROLE_ADMIN 권한이 필요합니다.
     * RoleHierarchy에 의해 ROLE_ADMIN은 ROLE_USER의 권한도 상속받습니다.
     *
     * <p><strong>보안 중요:</strong> context-path({@code /api})를 포함하면
     * 매칭되지 않아 {@code anyRequest().authenticated()}가 적용되어
     * ROLE_USER도 접근 가능한 심각한 보안 취약점이 발생합니다.
     * 반드시 servlet path인 {@code /admin/**}만 사용해야 합니다.
     */
    public static final String[] ADMIN_PATHS = {
            "/admin/**",
            "/actuator/info",      // Actuator info 엔드포인트 (관리자 전용)
            "/actuator/metrics",   // Actuator metrics 엔드포인트 (관리자 전용)
            "/actuator/prometheus" // Actuator prometheus 엔드포인트 (관리자 전용)
    };
}

