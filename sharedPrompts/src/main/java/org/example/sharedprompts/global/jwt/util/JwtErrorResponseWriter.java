package org.example.sharedprompts.global.jwt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.response.CustomResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * JWT 인증 관련 에러 응답을 작성하는 유틸리티 클래스
 * 
 * JwtAuthFilter와 JwtAuthenticationEntryPoint에서 일관된 형식으로
 * 에러 응답을 작성하기 위한 공통 로직을 제공합니다.
 */
public class JwtErrorResponseWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JwtErrorResponseWriter() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * ErrorCode만으로 에러 응답 작성
     * 
     * @param response HttpServletResponse
     * @param errorCode 에러 코드
     * @throws IOException 응답 작성 실패 시
     */
    public static void writeErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        writeErrorResponse(response, new ApiException(errorCode));
    }

    /**
     * ApiException을 기반으로 에러 응답을 HTTP 응답에 작성
     * 
     * @param response HttpServletResponse
     * @param apiException ApiException
     * @throws IOException 응답 작성 실패 시
     */
    private static void writeErrorResponse(
            HttpServletResponse response,
            ApiException apiException
    ) throws IOException {
        HttpStatus httpStatus = apiException.getErrorCode().getHttpStatus();
        response.setStatus(httpStatus.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(CustomResponse.fail(apiException))
        );
    }
}

