package org.example.sharedprompts.global.jwt.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    // 프론트 SPA 주소
    private static final String FRONT_REDIRECT_URL = "http://localhost:5173/oauth/failure";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // 실패 이유 로깅
        log.error("OAuth2 로그인 실패: {}", exception.getMessage());

        // 프론트로 실패 메시지 전달
        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        String redirectUrl = FRONT_REDIRECT_URL + "?error=" + errorMessage;

        response.sendRedirect(redirectUrl);
    }
}
