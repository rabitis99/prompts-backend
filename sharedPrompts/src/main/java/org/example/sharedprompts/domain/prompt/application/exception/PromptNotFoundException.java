package org.example.sharedprompts.domain.prompt.application.exception;

/** 프롬프트 미존재 */
public class PromptNotFoundException extends PromptDomainException {

    public PromptNotFoundException(Long promptId) {
        super("Prompt not found. id=" + promptId);
    }
}

