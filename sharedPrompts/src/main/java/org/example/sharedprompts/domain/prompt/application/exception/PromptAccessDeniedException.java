package org.example.sharedprompts.domain.prompt.application.exception;

/**
 * 프롬프트에 대한 접근/수정 권한이 없을 때 사용하는 예외.
 * <p>외부 노출용 메시지에는 식별자를 넣지 않고, 식별자는 구조화 로그용으로만 보관합니다.</p>
 */
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

