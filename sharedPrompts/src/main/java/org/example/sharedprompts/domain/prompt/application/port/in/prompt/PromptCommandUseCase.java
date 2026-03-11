package org.example.sharedprompts.domain.prompt.application.port.in.prompt;

import org.example.sharedprompts.domain.prompt.application.port.in.command.DeletePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UpdatePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;

/** 프롬프트 수정/삭제 유즈케이스 */
public interface PromptCommandUseCase {

    PromptDetailView updatePrompt(UpdatePromptCommand command);

    void deletePrompt(DeletePromptCommand command);
}
