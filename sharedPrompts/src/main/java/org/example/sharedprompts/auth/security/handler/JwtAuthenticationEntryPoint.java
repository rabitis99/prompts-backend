package org.example.sharedprompts.auth.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.jwt.util.JwtErrorResponseWriter;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패 시 처리하는 EntryPoint.
 * (예: 토큰 누락, 만료 등)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JwtErrorResponseWriter jwtErrorResponseWriter;

    /**
     * Sends a standardized 401 Unauthorized error response when authentication fails.
     *
     * @param response the HttpServletResponse to write the error payload to
     * @param authException the authentication failure that triggered this entry point
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // 일관된 형식으로 에러 응답 작성
        jwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }
}
