package org.example.sharedprompts.domain.prompt.application.exception;

/** 프롬프트 애플리케이션 계층 공통 예외 베이스 */
public class PromptDomainException extends RuntimeException {

    public PromptDomainException(String message) {
        super(message);
    }

    public PromptDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

