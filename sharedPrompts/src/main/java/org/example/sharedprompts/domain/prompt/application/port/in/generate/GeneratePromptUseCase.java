package org.example.sharedprompts.domain.prompt.application.port.in.generate;

import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;

/** 프롬프트 생성 유즈케이스. ConfirmedSemanticAxes 해석 후 호출 */
public interface GeneratePromptUseCase {

    GeneratePromptResult generate(GeneratePromptCommand command, ConfirmedSemanticAxes axes);
}
