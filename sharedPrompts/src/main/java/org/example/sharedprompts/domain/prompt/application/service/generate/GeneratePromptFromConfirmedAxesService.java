package org.example.sharedprompts.domain.prompt.application.service.generate;

import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.ConfirmedAxesMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptFromConfirmedAxesUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Generates a prompt from already-confirmed semantic axes.
 * Derives objective/outputNeeds via IntentDictionary and taskDomain via CategorySemanticProfileRegistry,
 * then builds ConfirmedSemanticAxes via ConfirmedAxesMapper (shape only), and delegates to GeneratePromptUseCase.
 */
@Service
public class GeneratePromptFromConfirmedAxesService implements GeneratePromptFromConfirmedAxesUseCase {

    private final ConfirmedAxesMapper confirmedAxesMapper;
    private final CategorySemanticProfileRegistry profileRegistry;
    private final GeneratePromptUseCase generatePromptUseCase;

    public GeneratePromptFromConfirmedAxesService(
            ConfirmedAxesMapper confirmedAxesMapper,
            CategorySemanticProfileRegistry profileRegistry,
            GeneratePromptUseCase generatePromptUseCase
    ) {
        this.confirmedAxesMapper = confirmedAxesMapper;
        this.profileRegistry = profileRegistry;
        this.generatePromptUseCase = generatePromptUseCase;
    }

    @Override
    public UnifiedGeneratePromptResult generate(ConfirmedGeneratePromptCommand command) {
        org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective domainObjective =
                resolveObjective(command);
        OutputNeeds outputNeeds = resolveOutputNeeds(command);
        TaskDomain taskDomain = resolveTaskDomain(command);

        ConfirmedSemanticAxes.Builder axesBuilder = confirmedAxesMapper.fromCommand(command);
        axesBuilder.objective(domainObjective).outputNeeds(outputNeeds).taskDomain(taskDomain);
        ConfirmedSemanticAxes axes = axesBuilder.build();

        GeneratePromptCommand v2Command = toV2Command(command, axes);
        GeneratePromptResult v2Result = generatePromptUseCase.generate(v2Command, axes);

        PromptObjective apiObjective = PromptObjective.fromDomainObjective(axes.objective());

        Map<String, String> axisSources = Map.of(
                "intent", AxisSourceConstants.USER_PROVIDED,
                "role", AxisSourceConstants.USER_PROVIDED,
                "action", AxisSourceConstants.USER_PROVIDED,
                "objective", AxisSourceConstants.RECOMMENDED,
                "output_needs", AxisSourceConstants.RECOMMENDED
        );

        return new UnifiedGeneratePromptResult(
                v2Result.generatedContent(),
                EngineMode.AUTO,
                EngineMode.V2,
                axes.category(),
                axes.taskDomain(),
                apiObjective,
                axes.outputNeeds(),
                axes.intent(),
                null,
                axes.role().orElse(null),
                axes.actionType().orElse(null),
                v2Result.badges(),
                v2Result.firstPassSuccess(),
                v2Result.repairCount(),
                v2Result.finallyPassed(),
                false,
                java.util.List.of(),
                EngineProfile.QUALITY_PIPELINE,
                axes.appliedProfileIds(),
                axes.validationWarnings(),
                axes.recommendationHints(),
                "confirmed",
                axisSources
        );
    }

    /** Derives objective from IntentDictionary; applies jsonSchema/extraction override. */
    private org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective resolveObjective(
            ConfirmedGeneratePromptCommand command
    ) {
        ActionIntent intent = command.intent();
        IntentDictionary.IntentResolutionDefaults defaults = IntentDictionary.getResolutionDefaults(intent);
        PromptObjective apiObjective = defaults.defaultObjective();
        if (command.jsonSchema() != null && !command.jsonSchema().isBlank() && intent == ActionIntent.EXTRACT) {
            apiObjective = PromptObjective.EXTRACTION;
        }
        return apiObjective.toDomainObjective();
    }

    /** Derives outputNeeds from IntentDictionary; applies jsonSchema override. */
    private OutputNeeds resolveOutputNeeds(ConfirmedGeneratePromptCommand command) {
        IntentDictionary.IntentResolutionDefaults defaults = IntentDictionary.getResolutionDefaults(command.intent());
        OutputNeeds outputNeeds = defaults.preferredOutputNeeds();
        if (command.jsonSchema() != null && !command.jsonSchema().isBlank()) {
            outputNeeds = OutputNeeds.JSON_SCHEMA_REQUIRED;
        }
        return outputNeeds;
    }

    /** Derives taskDomain from profile or category default. */
    private TaskDomain resolveTaskDomain(ConfirmedGeneratePromptCommand command) {
        Optional<CategorySemanticProfile> profileOpt = profileRegistry.getProfile(command.category());
        return profileOpt
                .map(CategorySemanticProfile::getBaseTaskDomain)
                .orElseGet(() -> command.category().getDefaultDomain());
    }

    private GeneratePromptCommand toV2Command(ConfirmedGeneratePromptCommand command, ConfirmedSemanticAxes axes) {
        String title = (command.title() != null && !command.title().isBlank())
                ? command.title()
                : "[Confirmed] " + axes.intent().name();
        String description = (command.description() != null && !command.description().isBlank())
                ? command.description()
                : null;
        return new GeneratePromptCommand(
                command.userId(),
                title,
                description,
                false,
                axes.category(),
                command.tags(),
                command.input(),
                axes.actionType().orElse(null),
                axes.role().orElse(null),
                axes.tone(),
                axes.style(),
                axes.language(),
                axes.experienceLevel(),
                false,
                command.jsonSchema()
        );
    }
}
