package org.example.sharedprompts.domain.prompt.application.port.in.command;

/**
 * 프롬프트 삭제 유즈케이스용 커맨드 모델.
 */
public record DeletePromptCommand(
        Long promptId,
        Long userId
) {
    public DeletePromptCommand {
        if (promptId == null) throw new IllegalArgumentException("promptId는 null일 수 없습니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 null일 수 없습니다.");
    }
}

