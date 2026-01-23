package org.example.sharedprompts.auth.oauth.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.security.config.properties.CookieSecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 보안 쿠키 관리 유틸리티
 * 
 * <p>OAuth2 및 인증 관련 쿠키를 안전하게 관리합니다.
 * HttpOnly, Secure, SameSite 속성을 일관되게 적용합니다.
 * 
 * <p>책임:
 * <ul>
 *   <li>보안 속성이 적용된 쿠키 추가</li>
 *   <li>보안 속성이 일치하는 쿠키 삭제</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SecureCookieManager {

    private final CookieSecurityProperties cookieSecurityProperties;

    /**
     * 보안 쿠키를 응답에 추가합니다.
     * 
     * <p><strong>보안 속성:</strong>
     * <ul>
     *   <li>HttpOnly: JavaScript 접근 차단</li>
     *   <li>Secure: HTTPS 전용 (환경별 설정)</li>
     *   <li>SameSite=LAX: CSRF 방지</li>
     * </ul>
     * 
     * @param response HttpServletResponse
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 쿠키 만료 시간(초)
     */
    public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie responseCookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge)
                .secure(cookieSecurityProperties.isSecure())
                .sameSite("Lax") // CSRF 방지를 위해 LAX 사용
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    /**
     * 보안 쿠키를 삭제합니다.
     * 
     * <p>쿠키 삭제 시 생성 시 설정한 것과 동일한 보안 속성을 설정해야 합니다.
     * 브라우저가 보안 속성이 다른 쿠키를 별개로 취급할 수 있기 때문입니다.
     * 
     * @param request HttpServletRequest (현재는 사용하지 않지만 호환성을 위해 유지)
     * @param response HttpServletResponse
     * @param name 삭제할 쿠키 이름
     */
    public void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        // ResponseCookie를 사용하여 쿠키 삭제 (생성 시와 동일한 속성 유지)
        ResponseCookie responseCookie = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .secure(cookieSecurityProperties.isSecure())
                .sameSite("Lax") // 생성 시와 동일한 SameSite 속성 유지
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }
}

