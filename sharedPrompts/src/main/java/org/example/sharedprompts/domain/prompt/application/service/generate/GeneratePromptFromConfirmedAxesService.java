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
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException;
import org.example.sharedprompts.domain.prompt.application.service.semantic.SemanticValidationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generates a prompt from already-confirmed semantic axes.
 * Derives objective/outputNeeds via IntentDictionary and taskDomain via CategorySemanticProfileRegistry,
 * then builds ConfirmedSemanticAxes via ConfirmedAxesMapper (shape only), and delegates to GeneratePromptUseCase.
 */
@Service
public class GeneratePromptFromConfirmedAxesService implements GeneratePromptFromConfirmedAxesUseCase {

    private final ConfirmedAxesMapper confirmedAxesMapper;
    private final CategorySemanticProfileRegistry profileRegistry;
    private final SemanticValidationService validationService;
    private final GeneratePromptUseCase generatePromptUseCase;

    private static final String DEFAULT_TITLE_PREFIX = "[Confirmed] ";

    public GeneratePromptFromConfirmedAxesService(
            ConfirmedAxesMapper confirmedAxesMapper,
            CategorySemanticProfileRegistry profileRegistry,
            SemanticValidationService validationService,
            GeneratePromptUseCase generatePromptUseCase
    ) {
        this.confirmedAxesMapper = confirmedAxesMapper;
        this.profileRegistry = profileRegistry;
        this.validationService = validationService;
        this.generatePromptUseCase = generatePromptUseCase;
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

        org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective domainObjective =
                resolveObjective(command);
        OutputNeeds outputNeeds = resolveOutputNeeds(command);
        TaskDomain taskDomain = resolveTaskDomain(command, profile);

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

        PromptObjective apiObjective = PromptObjective.fromDomainObjective(axes.objective());

        java.util.Map<String, String> axisSources = new java.util.HashMap<>();
        axisSources.put("intent", AxisSourceConstants.USER_PROVIDED);
        if (axes.role().isPresent()) {
            axisSources.put("role", AxisSourceConstants.USER_PROVIDED);
        }
        if (axes.actionType().isPresent()) {
            axisSources.put("action", AxisSourceConstants.USER_PROVIDED);
        }
        axisSources.put("objective", AxisSourceConstants.RECOMMENDED);
        axisSources.put("output_needs", AxisSourceConstants.RECOMMENDED);

        return new UnifiedGeneratePromptResult(
                v2Result.generatedContent(),
                EngineMode.AUTO,
                EngineMode.V2,
                axes.category(),
                axes.taskDomain(),
                apiObjective,
                axes.outputNeeds(),
                axes.intent(),
                null, // variant: not used in confirmed flow
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
    private TaskDomain resolveTaskDomain(ConfirmedGeneratePromptCommand command, CategorySemanticProfile profile) {
        if (profile != null) {
            return profile.getBaseTaskDomain();
        }
        return command.category().getDefaultDomain();
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
                /* isPublic = */ false,
                axes.category(),
                command.tags(),
                command.input(),
                axes.actionType().orElse(null),
                axes.role().orElse(null),
                axes.tone(),
                axes.style(),
                axes.language(),
                axes.experienceLevel(),
                /* experimentalEnabled = */ false,
                command.jsonSchema()
        );
    }
}
