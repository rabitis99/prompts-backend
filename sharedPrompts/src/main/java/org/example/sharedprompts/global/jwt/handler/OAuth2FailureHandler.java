package org.example.sharedprompts.global.jwt.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${oauth2.failure-redirect-url}")
    private String frontRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.error("OAuth2 로그인 실패 - URI: {}, IP: {}, 이유: {}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                exception.getMessage());

        String errorMessage = URLEncoder.encode("인증에 실패했습니다. 다시 시도해주세요.", StandardCharsets.UTF_8);
        String redirectUrl = frontRedirectUrl + "?error=" + errorMessage;

        response.sendRedirect(redirectUrl);
    }
}
