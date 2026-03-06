package org.example.sharedprompts.domain.prompt.application.exception;

/**
 * 프롬프트에 대한 접근/수정 권한이 없을 때 사용하는 예외.
 */
public class PromptAccessDeniedException extends PromptDomainException {

    public PromptAccessDeniedException(Long promptId, Long userId) {
        super("Access to prompt is forbidden. promptId=" + promptId + ", userId=" + userId);
    }
}

