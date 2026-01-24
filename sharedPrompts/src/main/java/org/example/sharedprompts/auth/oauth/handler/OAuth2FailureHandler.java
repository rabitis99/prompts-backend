package org.example.sharedprompts.auth.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 인증 실패 핸들러
 * 상세한 로깅과 Audit 이벤트를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final AuthAuditPublisher authAuditPublisher;

    @Value("${oauth2.failure-redirect-url}")
    private String frontRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String provider = extractProvider(request);
        String redirectUri = request.getRequestURI();
        String ip = HttpRequestUtils.getClientIpAddress(request);
        String userAgent = HttpRequestUtils.getUserAgent(request);
        String errorCode = extractErrorCode(exception);

        // 상세 로그
        log.error("OAuth2 login failed - provider={}, exceptionType={}, code={}, redirectUri={}, ip={}, ua={}",
                provider,
                exception.getClass().getSimpleName(),
                errorCode,
                redirectUri,
                ip,
                userAgent);

        // Audit 이벤트 발행
        authAuditPublisher.loginFailWithoutPrincipal(
                AuthFailReason.OAUTH2_AUTHENTICATION_FAILED
        );

        String errorMessage = URLEncoder.encode("인증에 실패했습니다. 다시 시도해주세요.", StandardCharsets.UTF_8);
        String redirectUrl = frontRedirectUrl + "?error=" + errorMessage;

        response.sendRedirect(redirectUrl);
    }

    /**
     * 요청에서 Provider 추출
     */
    private String extractProvider(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/google")) {
            return "GOOGLE";
        } else if (uri.contains("/kakao")) {
            return "KAKAO";
        } else if (uri.contains("/naver")) {
            return "NAVER";
        }
        return "UNKNOWN";
    }

    /**
     * 예외에서 에러 코드 추출
     */
    private String extractErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) exception).getError();
            return error != null ? error.getErrorCode() : "UNKNOWN";
        }
        return exception.getClass().getSimpleName();
    }
}

