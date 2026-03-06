package org.example.sharedprompts.domain.prompt.application.exception;

/**
 * 프롬프트를 찾을 수 없을 때 사용하는 예외.
 */
public class PromptNotFoundException extends PromptDomainException {

    public PromptNotFoundException(Long promptId) {
        super("Prompt not found. id=" + promptId);
    }
}

