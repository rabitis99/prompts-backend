package org.example.sharedprompts.global.exception;

/**
 * LLM 호출 결과가 비어있거나 유효하지 않을 때 사용하는 도메인 특화 예외.
 * GlobalExceptionHandler에서 503 Service Unavailable로 매핑된다.
 */
public class LLMResponseException extends RuntimeException {

    public LLMResponseException(String message) {
        super(message);
    }

    public LLMResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
