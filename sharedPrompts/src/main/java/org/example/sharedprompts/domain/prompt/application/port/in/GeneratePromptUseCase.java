package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;

/**
 * 프롬프트 생성 유즈케이스 포트 — 4단계 파이프라인(Clarify → Solve → Verify → Repair)의 진입점.
 *
 * <p><b>Semantic pipeline:</b> The only production entry point is
 * {@link #generate(GeneratePromptCommand, ConfirmedSemanticAxes)}.
 * Callers must resolve to {@link ConfirmedSemanticAxes} via
 * {@link org.example.sharedprompts.domain.prompt.application.service.semantic.SemanticResolutionService}
 * before invoking this use case. No overload without axes is provided; this prevents flat enum-combination semantics.</p>
 */
public interface GeneratePromptUseCase {

    /**
     * 확정된 의미 축으로 프롬프트를 생성한다 (category → intent → role/action pipeline).
     * This is the only entry point. Resolve semantics via SemanticResolutionService first.
     */
    GeneratePromptResult generate(GeneratePromptCommand command, ConfirmedSemanticAxes axes);
}
