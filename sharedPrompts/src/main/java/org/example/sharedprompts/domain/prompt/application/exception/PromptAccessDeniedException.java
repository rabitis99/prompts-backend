package org.example.sharedprompts.domain.prompt.application.exception;

/** 프롬프트 접근/수정 권한 없음 */
public class PromptAccessDeniedException extends PromptDomainException {

    private final Long promptId;
    private final Long userId;

    public PromptAccessDeniedException(Long promptId, Long userId) {
        super("Access to prompt is forbidden.");
        this.promptId = promptId;
        this.userId = userId;
    }

    public Long getPromptId() {
        return promptId;
    }

    public Long getUserId() {
        return userId;
    }
}

