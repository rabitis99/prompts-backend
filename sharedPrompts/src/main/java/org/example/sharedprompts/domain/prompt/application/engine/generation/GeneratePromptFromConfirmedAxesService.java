package org.example.sharedprompts.domain.prompt.application.engine.generation;

import org.example.sharedprompts.domain.prompt.application.mapping.ConfirmedAxesMapper;
import org.example.sharedprompts.domain.prompt.application.policy.AxisSourcePolicy;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptFromConfirmedAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.semantic.IntentBasedAxisDefaultsResolver;
import org.example.sharedprompts.domain.prompt.application.semantic.validation.SemanticValidationService;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.example.sharedprompts.domain.prompt.application.engine.contract.SchemaContractEvaluator;
import org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException;
import org.springframework.stereotype.Service;

import java.util.List;

/** 확정 축 기반 프롬프트 생성. 검증·축 구성 후 GeneratePromptUseCase 위임 */
@Service
public class GeneratePromptFromConfirmedAxesService implements GeneratePromptFromConfirmedAxesUseCase {

    private final ConfirmedAxesMapper confirmedAxesMapper;
    private final CategorySemanticProfileRegistry profileRegistry;
    private final SemanticValidationService validationService;
    private final IntentBasedAxisDefaultsResolver axisDefaultsResolver;
    private final GeneratePromptUseCase generatePromptUseCase;
    private final AxisSourcePolicy axisSourcePolicy;
    private final UnifiedGeneratePromptResultBuilder resultBuilder;
    private final SchemaContractEvaluator schemaContractEvaluator;

    private static final String DEFAULT_TITLE_PREFIX = "[Confirmed] ";
    private static final boolean IS_NOT_PUBLIC = false;
    private static final boolean EXPERIMENTAL_DISABLED = false;

    public GeneratePromptFromConfirmedAxesService(
            ConfirmedAxesMapper confirmedAxesMapper,
            CategorySemanticProfileRegistry profileRegistry,
            SemanticValidationService validationService,
            IntentBasedAxisDefaultsResolver axisDefaultsResolver,
            GeneratePromptUseCase generatePromptUseCase,
            AxisSourcePolicy axisSourcePolicy,
            UnifiedGeneratePromptResultBuilder resultBuilder,
            SchemaContractEvaluator schemaContractEvaluator
    ) {
        this.confirmedAxesMapper = confirmedAxesMapper;
        this.profileRegistry = profileRegistry;
        this.validationService = validationService;
        this.axisDefaultsResolver = axisDefaultsResolver;
        this.generatePromptUseCase = generatePromptUseCase;
        this.axisSourcePolicy = axisSourcePolicy;
        this.resultBuilder = resultBuilder;
        this.schemaContractEvaluator = schemaContractEvaluator;
    }

    @Override
    public UnifiedGeneratePromptResult generate(ConfirmedGeneratePromptCommand command) {
        if (command.category() == org.example.sharedprompts.domain.prompt.common.enums.PromptCategory.EXTRACTION) {
            if (command.requestMode() != org.example.sharedprompts.domain.prompt.common.enums.RequestMode.EXTRACTION) {
                throw new SemanticResolutionException(List.of("EXTRACTION category is only valid with request_mode=EXTRACTION"));
            }
        }

        CategorySemanticProfile profile = profileRegistry.getProfile(command.category()).orElse(null);
        SemanticValidationResult validation = validationService.validate(command, profile);

        if (validation.severity() == SemanticValidationResult.Severity.ERROR) {
            List<String> messages = validation.items().stream()
                    .map(i -> i.code() + ": " + i.message())
                    .toList();
            throw new SemanticResolutionException(messages);
        }

        boolean hasJsonSchema = command.jsonSchema() != null && !command.jsonSchema().isBlank();
        boolean extractionRequestMode = command.requestMode() == RequestMode.EXTRACTION;
        org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective domainObjective =
                axisDefaultsResolver.resolveObjective(command.intent(), hasJsonSchema, extractionRequestMode);
        var outputNeeds = axisDefaultsResolver.resolveOutputNeeds(command.intent(), hasJsonSchema);
        var taskDomain = axisDefaultsResolver.resolveTaskDomain(profile, command.category());

        ConfirmedSemanticAxes.Builder axesBuilder = confirmedAxesMapper.fromCommand(command);
        axesBuilder.objective(domainObjective).outputNeeds(outputNeeds).taskDomain(taskDomain);
        
        if (validation.severity() == SemanticValidationResult.Severity.WARNING) {
            List<String> warnings = validation.items().stream()
                    .map(SemanticValidationResult.SemanticValidationItem::message)
                    .toList();
            axesBuilder.validationWarnings(warnings);
        }
        
        ConfirmedSemanticAxes axes = axesBuilder.build();

        GeneratePromptCommand v2Command = toV2Command(command, axes);
        GeneratePromptResult v2Result = generatePromptUseCase.generate(v2Command, axes);

        var schemaEval = schemaContractEvaluator.evaluate(v2Result);
        var axisSources = axisSourcePolicy.forConfirmedAxes(axes);

        return resultBuilder.build(
                v2Result,
                axes,
                EngineMode.AUTO,
                null,
                schemaEval,
                axisSources,
                "confirmed"
        );
    }

    private GeneratePromptCommand toV2Command(ConfirmedGeneratePromptCommand command, ConfirmedSemanticAxes axes) {
        String title = (command.title() != null && !command.title().isBlank())
                ? command.title()
                : DEFAULT_TITLE_PREFIX + axes.intent().name();
        String description = (command.description() != null && !command.description().isBlank())
                ? command.description()
                : null;
        return new GeneratePromptCommand(
                command.userId(),
                title,
                description,
                IS_NOT_PUBLIC,
                axes.category(),
                command.tags(),
                command.input(),
                axes.actionType().orElse(null),
                axes.role().orElse(null),
                axes.tone(),
                axes.style(),
                axes.language(),
                axes.experienceLevel(),
                EXPERIMENTAL_DISABLED,
                command.jsonSchema()
        );
    }
}
