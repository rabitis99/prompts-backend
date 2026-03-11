package org.example.sharedprompts.domain.prompt.application.exception;

/** 프롬프트 수정 요청 유효하지 않음 */
public class InvalidPromptUpdateException extends PromptDomainException {

    public InvalidPromptUpdateException(String message) {
        super(message);
    }

    public InvalidPromptUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}

