package org.example.sharedprompts.auth.jwt.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * JWT 인증 관련 에러 응답을 작성하는 서비스 클래스
 *
 * JwtAuthFilter와 JwtAuthenticationEntryPoint에서 일관된 형식으로
 * 에러 응답을 작성하기 위한 공통 로직을 제공합니다.
 *
 * Spring의 JacksonConfig와 동일한 설정을 사용하여 응답 형식 일관성을 보장합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * ErrorCode만으로 에러 응답 작성
     *
     * @param response HttpServletResponse
     * @param errorCode 에러 코드
     * @throws IOException 응답 작성 실패 시
     */
    public void writeErrorResponse(
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
    private void writeErrorResponse(
            HttpServletResponse response,
            ApiException apiException
    ) throws IOException {
        HttpStatus httpStatus = apiException.getErrorCode().getHttpStatus();
        response.setStatus(httpStatus.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(
                objectMapper.writeValueAsString(CustomResponse.fail(apiException))
        );
        writer.flush();
    }
}