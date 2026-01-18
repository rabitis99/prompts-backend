package org.example.sharedprompts.global.jwt.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.servlet.http.HttpServletResponse;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.response.CustomResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JWT 인증 관련 에러 응답을 작성하는 유틸리티 클래스
 * 
 * JwtAuthFilter와 JwtAuthenticationEntryPoint에서 일관된 형식으로
 * 에러 응답을 작성하기 위한 공통 로직을 제공합니다.
 * 
 * Spring의 JacksonConfig와 동일한 설정을 사용하여 응답 형식 일관성을 보장합니다.
 */
public class JwtErrorResponseWriter {

    private static final ObjectMapper objectMapper = createObjectMapper();

    private JwtErrorResponseWriter() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Spring의 JacksonConfig와 동일한 설정을 가진 ObjectMapper 생성
     * 응답 형식 일관성을 보장하기 위해 동일한 설정을 적용합니다.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // JavaTimeModule 등록 (LocalDateTime 직렬화/역직렬화)
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.registerModule(javaTimeModule);

        // Spring JacksonConfig와 동일한 설정 적용
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
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
        response.getWriter().flush();
    }
}

