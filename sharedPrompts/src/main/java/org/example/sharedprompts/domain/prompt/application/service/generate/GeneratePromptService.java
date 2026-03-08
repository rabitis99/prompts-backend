package org.example.sharedprompts.domain.prompt.application.service.generate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.LLMClientPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.service.badge.BadgeResolver;
import org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecFactory;
import org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecValidator;
import org.springframework.stereotype.Service;

/**
 * 4단계 파이프라인: Clarify(axes) → Solve → Verify → Repair.
 *
 * <p><b>Semantic pipeline:</b> Spec is built only via
 * {@link PromptSpecFactory#createFromConfirmedAxes(ConfirmedSemanticAxes, String, String)}.
 * Callers must supply {@link ConfirmedSemanticAxes} from SemanticResolutionService; no bypass path exists.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratePromptService implements org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase {

    private static final int MAX_REPAIRS = 2;

    private final PromptSpecFactory promptSpecFactory;
    private final PromptSpecValidator promptSpecValidator;
    private final LLMClientPort llmClient;
    private final SavePromptVersionPort savePromptVersionPort;
    private final BadgeResolver badgeResolver;

    @Override
    public GeneratePromptResult generate(GeneratePromptCommand command, ConfirmedSemanticAxes axes) {
        PromptSpec spec = promptSpecFactory.createFromConfirmedAxes(
                axes,
                command.input(),
                command.jsonSchema()
        );
        return runPipeline(command, spec);
    }

    private GeneratePromptResult runPipeline(GeneratePromptCommand command, PromptSpec spec) {
        String draft = llmClient.solve(spec);
        VerifyResult firstVerify = promptSpecValidator.verify(draft, spec);
        boolean firstPassSuccess = firstVerify.isPassed();
        int repairCount = 0;
        String current = draft;
        VerifyResult lastResult = firstVerify;

        while (!lastResult.isPassed() && repairCount < MAX_REPAIRS) {
            current = llmClient.repair(
                    current,
                    spec,
                    lastResult.getFailedItems(),
                    lastResult.getFailureReasons()
            );
            repairCount++;
            lastResult = promptSpecValidator.verify(current, spec);
        }

        boolean finallyPassed = lastResult.isPassed();
        Long promptId = savePromptVersionPort.save(
                command,
                spec,
                current,
                repairCount,
                finallyPassed
        );

        boolean formatValid = Boolean.TRUE.equals(lastResult.getItemResults().get(
                org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric.RubricItem.FORMAT_COMPLIANCE));

        var badges = badgeResolver.resolve(lastResult, firstPassSuccess, repairCount, finallyPassed);

        return new GeneratePromptResult(
                promptId,
                command.title() != null && !command.title().isBlank() ? command.title() : "Untitled",
                current,
                badges,
                spec.getObjective(),
                formatValid,
                firstPassSuccess,
                repairCount,
                finallyPassed
        );
    }
}
