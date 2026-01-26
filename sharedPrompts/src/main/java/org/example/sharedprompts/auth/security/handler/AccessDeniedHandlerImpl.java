package org.example.sharedprompts.auth.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.jwt.util.JwtErrorResponseWriter;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인가(권한 부족) 실패 처리 핸들러.
 *
 * - 접근 권한이 없는 리소스에 대한 시도를 로깅합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final JwtErrorResponseWriter jwtErrorResponseWriter;

    /**
     * Handles an access-denied event by logging the denial and sending a forbidden error response to the client.
     *
     * @param request the incoming HTTP request that was denied access
     * @param response the HTTP response used to return the error to the client
     * @param accessDeniedException the exception describing why access was denied
     * @throws IOException if an I/O error occurs while writing the response
     * @throws ServletException if a servlet error occurs while handling the denial
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        log.warn("Access denied - uri={}, method={}, message={}",
                request.getRequestURI(),
                request.getMethod(),
                accessDeniedException.getMessage());

        jwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.FORBIDDEN);
    }
}