package org.example.sharedprompts.auth.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

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

        JwtErrorResponseWriter.writeErrorResponse(response, ErrorCode.FORBIDDEN);
    }
}

