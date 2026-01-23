package org.example.sharedprompts.auth.security.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 쿠키 보안 설정 프로퍼티
 * 
 * OAuth2 AuthorizationRequest 쿠키의 보안 설정을 관리합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "auth.cookie")
public class CookieSecurityProperties {

    /**
     * 쿠키 Secure 플래그 설정
     * 
     * <p>true: HTTPS 환경에서만 쿠키 전송 (운영 환경 권장)
     * false: HTTP/HTTPS 모두에서 쿠키 전송 (개발 환경)
     * 
     * <p>기본값: false (개발 환경 호환성)
     * 
     * <p>설정 방법:
     * <ul>
     *   <li>application.yml: auth.cookie.secure=true</li>
     *   <li>환경 변수: AUTH_COOKIE_SECURE=true</li>
     * </ul>
     * 
     * <p>주의: SameSite 속성은 Java Cookie API에서 직접 지원하지 않으므로
     * 서블릿 컨테이너 레벨에서 설정하거나 ResponseCookie를 사용해야 합니다.
     */
    private boolean secure = false;
}

