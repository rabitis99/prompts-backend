package org.example.sharedprompts.auth.rate.filter.builder.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Rate Limit 초과 시 응답 작성 유틸리티
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitResponseWriter {

    /**
     * HTTP 헤더 이름 상수
     */
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String CHARACTER_ENCODING = "UTF-8";

    /**
     * Rate Limit 초과 응답을 작성합니다.
     * 
     * @param response HttpServletResponse
     * @param objectMapper ObjectMapper
     * @param retryAfterSeconds Retry-After 헤더 값 (초)
     * @param errorCode 사용할 에러 코드 (null이면 기본값 RATE_LIMIT_EXCEEDED 사용)
     * @throws IOException 응답 작성 실패 시
     */
    public static void writeTooManyRequests(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            long retryAfterSeconds,
            ErrorCode errorCode
    ) throws IOException {
        ErrorCode code = (errorCode != null) ? errorCode : ErrorCode.RATE_LIMIT_EXCEEDED;
        ApiException exception = new ApiException(code);
        CustomResponse<Void> body = CustomResponse.fail(exception);

        // HTTP 상태 코드 설정
        response.setStatus(code.getHttpStatus().value());
        
        // Retry-After 헤더 설정 (RFC 7231)
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
        
        // Content-Type 및 인코딩 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(CHARACTER_ENCODING);

        // 응답 본문 작성
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Rate Limit 초과 응답을 작성합니다. (기본 에러 코드 사용)
     * 
     * @param response HttpServletResponse
     * @param objectMapper ObjectMapper
     * @param retryAfterSeconds Retry-After 헤더 값 (초)
     * @throws IOException 응답 작성 실패 시
     */
    public static void writeTooManyRequests(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            long retryAfterSeconds
    ) throws IOException {
        writeTooManyRequests(response, objectMapper, retryAfterSeconds, null);
    }
}


