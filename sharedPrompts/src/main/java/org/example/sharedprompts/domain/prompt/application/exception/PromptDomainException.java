package org.example.sharedprompts.domain.prompt.application.exception;

/**
 * 프롬프트 애플리케이션 계층의 공통 예외 베이스.
 *
 * <p>웹/전송 계층에 종속되지 않도록 HTTP 상태나 ErrorCode 에 직접 의존하지 않는다.</p>
 */
public class PromptDomainException extends RuntimeException {

    public PromptDomainException(String message) {
        super(message);
    }

    public PromptDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

