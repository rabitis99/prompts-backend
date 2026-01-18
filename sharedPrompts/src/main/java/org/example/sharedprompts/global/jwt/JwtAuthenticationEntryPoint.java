package org.example.sharedprompts.global.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.jwt.util.JwtErrorResponseWriter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // 일관된 형식으로 에러 응답 작성
        JwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }
}
