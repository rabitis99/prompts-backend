package org.example.sharedprompts.auth.oauth.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.util.SecureCookieManager;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.util.concurrent.TimeUnit;

/**
 * 쿠키 기반 OAuth2 AuthorizationRequest 저장소
 * 
 * <p>STATELESS 세션 정책과 호환되도록 쿠키에 AuthorizationRequest를 저장합니다.
 * HttpSession을 사용하지 않으므로 STATELESS 모드에서도 OAuth2 로그인이 정상 작동합니다.
 * 
 * <p>보안 고려사항:
 * <ul>
 *   <li>쿠키는 HttpOnly, Secure 속성을 사용하여 보안을 강화합니다.</li>
 *   <li>쿠키 값은 Base64 인코딩된 JSON 문자열입니다 (Java 직렬화 대신 JSON 사용).</li>
 *   <li>Spring Security의 SecurityJackson2Modules를 사용하여 역직렬화 공격을 방지합니다.</li>
 *   <li>쿠키 만료 시간은 10분으로 설정합니다.</li>
 *   <li>SameSite 속성은 SecureCookieManager에서 Lax로 설정됩니다.</li>
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

    private final OAuth2AuthorizationRequestSerializer serializer;
    private final SecureCookieManager cookieManager;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookie == null) {
            return null;
        }

        try {
            return serializer.deserialize(cookie.getValue());
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
            cookieManager.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            cookieManager.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        try {
            String serialized = serializer.serialize(authorizationRequest);
            cookieManager.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

            String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
            if (redirectUriAfterLogin != null && !redirectUriAfterLogin.isEmpty()) {
                cookieManager.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
            }
        } catch (ApiException e) {
            // ApiException은 직렬화 실패 등 치명적인 오류이므로 다시 던져서 OAuth2 플로우를 중단
            log.error("OAuth2 AuthorizationRequest 쿠키 저장 실패: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // 기타 예외(예: IOException)도 OAuth2 플로우에 영향을 주므로 다시 던짐
            log.error("OAuth2 AuthorizationRequest 쿠키 저장 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, 
                    "OAuth2 AuthorizationRequest 쿠키 저장 실패", e);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            cookieManager.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            cookieManager.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        }
        return authorizationRequest;
    }
}


