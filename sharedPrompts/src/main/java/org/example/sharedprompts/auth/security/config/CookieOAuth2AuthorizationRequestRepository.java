package org.example.sharedprompts.auth.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
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
 *   <li>쿠키는 HttpOnly, Secure 속성을 사용하여 보안을 강화합니다.</li>
 *   <li>쿠키 값은 Base64 인코딩된 JSON 문자열입니다 (Java 직렬화 대신 JSON 사용).</li>
 *   <li>Spring Security의 SecurityJackson2Modules를 사용하여 역직렬화 공격을 방지합니다.</li>
 *   <li>쿠키 만료 시간은 10분으로 설정합니다.</li>
 *   <li>SameSite 속성: Jakarta Servlet Cookie API는 SameSite를 직접 지원하지 않습니다.
 *       운영 환경에서는 다음 방법 중 하나를 사용하여 SameSite를 설정해야 합니다:
 *       <ul>
 *         <li>ResponseCookie 사용: {@code org.springframework.http.ResponseCookie}를 사용하여
 *             SameSite 속성을 명시적으로 설정</li>
 *         <li>서블릿 컨테이너 설정: Tomcat의 경우 {@code server.servlet.session.cookie.same-site}
 *             설정을 통해 전역적으로 적용</li>
 *       </ul>
 *   </li>
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
    
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        // Spring Security의 SecurityJackson2Modules를 사용하여 안전한 직렬화 설정
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        this.objectMapper = mapper;
    }

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

    /**
     * 쿠키를 응답에 추가합니다.
     * 
     * <p><strong>SameSite 속성 설정:</strong>
     * Jakarta Servlet Cookie API는 SameSite를 직접 지원하지 않습니다.
     * 운영 환경에서 SameSite를 설정하려면:
     * <ul>
     *   <li>ResponseCookie 사용: {@code org.springframework.http.ResponseCookie.from(name, value)
     *       .sameSite(SameSite.LAX).build()}를 사용하여 쿠키를 생성하고
     *       {@code response.addHeader("Set-Cookie", responseCookie.toString())}로 추가</li>
     *   <li>서블릿 컨테이너 설정: application.yml에 {@code server.servlet.session.cookie.same-site=lax} 설정</li>
     * </ul>
     * 
     * @param response HttpServletResponse
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 쿠키 만료 시간(초)
     */
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        // 환경별 Secure 플래그 설정 (운영 환경에서는 true로 설정)
        cookie.setSecure(cookieSecurityProperties.isSecure());
        // 참고: SameSite 속성은 Jakarta Servlet Cookie API에서 직접 지원하지 않으므로
        // ResponseCookie 사용 또는 서블릿 컨테이너 설정을 통해 적용해야 합니다.
        response.addCookie(cookie);
    }

    /**
     * 쿠키를 삭제합니다.
     * 
     * <p>쿠키 삭제 시 생성 시 설정한 것과 동일한 보안 속성을 설정해야 합니다.
     * 브라우저가 보안 속성이 다른 쿠키를 별개로 취급할 수 있기 때문입니다.
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param name 삭제할 쿠키 이름
     */
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        if (cookie != null) {
            cookie.setValue("");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            // 생성 시 설정한 것과 동일한 보안 속성 설정 (브라우저 호환성)
            cookie.setHttpOnly(true);
            cookie.setSecure(cookieSecurityProperties.isSecure());
            response.addCookie(cookie);
        }
    }

    /**
     * OAuth2AuthorizationRequest를 JSON으로 직렬화합니다.
     * 
     * <p>Spring Security의 SecurityJackson2Modules를 사용하여 안전하게 직렬화합니다.
     * Java 기본 직렬화 대신 JSON을 사용하여 역직렬화 공격을 방지합니다.
     * 
     * @param object 직렬화할 객체 (OAuth2AuthorizationRequest)
     * @return Base64 URL 인코딩된 JSON 문자열
     */
    private String serialize(OAuth2AuthorizationRequest object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("OAuth2AuthorizationRequest 직렬화 실패", e);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, null, "OAuth2AuthorizationRequest 직렬화 실패", e);
        }
    }

    /**
     * Base64 URL 인코딩된 JSON 문자열을 OAuth2AuthorizationRequest로 역직렬화합니다.
     * 
     * <p>Spring Security의 SecurityJackson2Modules를 사용하여 안전하게 역직렬화합니다.
     * Java 기본 직렬화 대신 JSON을 사용하여 역직렬화 공격을 방지합니다.
     * 
     * @param cookie Base64 URL 인코딩된 JSON 문자열
     * @param clazz 역직렬화할 클래스
     * @return 역직렬화된 객체 (실패 시 null)
     */
    private <T> T deserialize(String cookie, Class<T> clazz) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie);
            String json = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("OAuth2AuthorizationRequest 역직렬화 실패", e);
            return null; // 역직렬화 실패 시 null 반환 (loadAuthorizationRequest에서 처리)
        }
    }
}

