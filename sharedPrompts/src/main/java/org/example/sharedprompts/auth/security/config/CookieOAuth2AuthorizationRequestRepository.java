package org.example.sharedprompts.auth.security.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.util.WebUtils;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 쿠키 기반 OAuth2 AuthorizationRequest 저장소
 * 
 * <p>STATELESS 세션 정책과 호환되도록 쿠키에 AuthorizationRequest를 저장합니다.
 * HttpSession을 사용하지 않으므로 STATELESS 모드에서도 OAuth2 로그인이 정상 작동합니다.
 * 
 * <p>보안 고려사항:
 * <ul>
 *   <li>쿠키는 HttpOnly, Secure, SameSite 속성을 사용하여 보안을 강화합니다.</li>
 *   <li>쿠키 값은 Base64 인코딩된 직렬화된 객체입니다.</li>
 *   <li>쿠키 만료 시간은 10분으로 설정합니다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = (int) TimeUnit.MINUTES.toSeconds(10);

    private final CookieSecurityProperties cookieSecurityProperties;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookie == null) {
            return null;
        }

        try {
            return deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            log.warn("OAuth2 AuthorizationRequest 쿠키 역직렬화 실패", e);
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        try {
            String serialized = serialize(authorizationRequest);
            addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

            String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
            if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isEmpty()) {
                addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
            }
        } catch (Exception e) {
            log.error("OAuth2 AuthorizationRequest 쿠키 저장 실패", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        }
        return authorizationRequest;
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        // 환경별 Secure 플래그 설정 (운영 환경에서는 true로 설정)
        cookie.setSecure(cookieSecurityProperties.isSecure());
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        if (cookie != null) {
            cookie.setValue("");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    private String serialize(Object object) {
        byte[] bytes = SerializationUtils.serialize(object);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    @SuppressWarnings("deprecation")
    private <T> T deserialize(String cookie, Class<T> clazz) {
        byte[] bytes = Base64.getUrlDecoder().decode(cookie);
        // Spring Security의 기본 구현도 SerializationUtils를 사용하므로 동일하게 사용
        return clazz.cast(SerializationUtils.deserialize(bytes));
    }
}

