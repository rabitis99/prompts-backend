package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;

/**
 * 프롬프트 수정/삭제 유즈케이스 포트.
 *
 * <p>수정 시 갱신된 상세 뷰를 반환하고, 삭제는 void이다.</p>
 */
public interface PromptCommandUseCase {

    /**
     * 프롬프트를 수정하고 갱신된 상세 뷰를 반환한다.
     */
    PromptDetailView updatePrompt(UpdatePromptCommand command);

    /**
     * 프롬프트를 삭제한다. 소유자만 삭제 가능하다.
     */
    void deletePrompt(DeletePromptCommand command);
}

