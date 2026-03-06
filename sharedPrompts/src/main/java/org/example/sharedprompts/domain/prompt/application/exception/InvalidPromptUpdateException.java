package org.example.sharedprompts.domain.prompt.application.exception;

/**
 * 프롬프트 수정 요청이 유효하지 않을 때 사용하는 예외.
 */
public class InvalidPromptUpdateException extends PromptDomainException {

    public InvalidPromptUpdateException(String message) {
        super(message);
    }
}

