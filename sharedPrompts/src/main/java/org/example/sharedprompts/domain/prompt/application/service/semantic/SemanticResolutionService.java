package org.example.sharedprompts.domain.prompt.application.service.semantic;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves command to confirmed semantic axes using profile → recommend → validate.
 * EXTRACTION request_type forces intent=EXTRACT, category=null handled; SIMPLE/ADVANCED require category+intent (or fail).
 *
 * <p><b>Semantic resolution order:</b> Category → ActionIntent → RoleType/ActionType (from profile + recommend).
 * ToneType, StyleType, and output format (OutputNeeds/jsonSchema) do <em>not</em> drive resolution—they are
 * passed through to ConfirmedSemanticAxes and applied only after semantic axes are fixed.</p>
 */
@Service
public class SemanticResolutionService {

    private final CategorySemanticProfileRegistry profileRegistry;
    private final SemanticRecommendationService recommendationService;
    private final SemanticValidationService validationService;

    public SemanticResolutionService(
            CategorySemanticProfileRegistry profileRegistry,
            SemanticRecommendationService recommendationService,
            SemanticValidationService validationService
    ) {
        this.profileRegistry = profileRegistry;
        this.recommendationService = recommendationService;
        this.validationService = validationService;
    }

    /**
     * Resolves command to confirmed semantic axes.
     * Uses {@link org.example.sharedprompts.domain.prompt.common.enums.RequestMode} from the command;
     * EXTRACTION mode forces intent=EXTRACT; SIMPLE/ADVANCED require category and intent (or profile fallback).
     *
     * @return ConfirmedSemanticAxes, or null if validation failed (caller should return 400 with validation result).
     */
    public Result resolve(UnifiedGeneratePromptCommand command) {
        if (command.requestMode() == RequestMode.EXTRACTION) {
            return resolveExtraction(command);
        }

        PromptCategory category = command.category();
        ActionIntent intent = command.intent();

        if (category == null) {
            return Result.fail(List.of("category is required for SIMPLE/ADVANCED"));
        }

        boolean fallbackIntentUsed = false;
        if (intent == null) {
            CategorySemanticProfile profileForFallback = profileRegistry.getProfile(category);
            if (profileForFallback != null && profileForFallback.getFallbackIntent() != null) {
                intent = profileForFallback.getFallbackIntent();
                fallbackIntentUsed = true;
            } else {
                return Result.fail(List.of("intent is required for SIMPLE/ADVANCED"));
            }
        }

        CategorySemanticProfile profile = profileRegistry.getProfile(category);
        SemanticValidationResult validation = validationService.validate(command, profile, intent);
        if (validation.severity() == SemanticValidationResult.Severity.ERROR || !validation.valid()) {
            List<String> messages = validation.items().stream()
                    .map(i -> i.code() + ": " + i.message())
                    .toList();
            return Result.fail(messages);
        }

        var recommendation = recommendationService.recommend(
                category,
                intent,
                profile,
                command.roleType(),
                command.actionType(),
                fallbackIntentUsed
        );

        IntentDictionary.IntentResolutionDefaults intentDefaults = IntentDictionary.getResolutionDefaults(intent);
        org.example.sharedprompts.domain.prompt.common.enums.PromptObjective apiObjective = intentDefaults.defaultObjective();
        OutputNeeds outputNeeds = intentDefaults.preferredOutputNeeds();
        if (command.jsonSchema() != null && !command.jsonSchema().isBlank()) {
            apiObjective = org.example.sharedprompts.domain.prompt.common.enums.PromptObjective.EXTRACTION;
            outputNeeds = OutputNeeds.JSON_SCHEMA_REQUIRED;
        }

        PromptObjective domainObjective = apiObjective.toDomainObjective();
        TaskDomain taskDomain = profile != null
                ? profile.getBaseTaskDomain()
                : category.getDefaultDomain();

        List<String> appliedIds = new ArrayList<>();
        appliedIds.add("profile:" + category.name());
        appliedIds.add("intent:" + intent.name());
        if (validation.severity() == SemanticValidationResult.Severity.WARNING) {
            appliedIds.add("validation:warnings");
        }

        List<String> warnings = new ArrayList<>();
        if (validation.severity() == SemanticValidationResult.Severity.WARNING) {
            validation.items().forEach(i -> warnings.add(i.message()));
        }

        ConfirmedSemanticAxes axes = ConfirmedSemanticAxes.builder()
                .category(category)
                .taskDomain(taskDomain)
                .intent(intent)
                .objective(domainObjective)
                .outputNeeds(outputNeeds)
                .role(recommendation.recommendedRole().orElse(null))
                .actionType(recommendation.recommendedAction().orElse(null))
                .tone(command.tone())
                .style(command.style())
                .language(command.language())
                .experienceLevel(command.experience())
                .appliedProfileIds(appliedIds)
                .validationWarnings(warnings)
                .recommendationHints(recommendation.recommendationHints())
                .build();

        return Result.ok(axes);
    }

    private Result resolveExtraction(UnifiedGeneratePromptCommand command) {
        ActionIntent intent = ActionIntent.EXTRACT;
        TaskDomain taskDomain = TaskDomain.ANALYTICAL;
        PromptObjective domainObjective = PromptObjective.EXTRACTION;
        OutputNeeds outputNeeds = OutputNeeds.JSON_SCHEMA_REQUIRED;

        ConfirmedSemanticAxes axes = ConfirmedSemanticAxes.builder()
                .category(PromptCategory.ETC)
                .taskDomain(taskDomain)
                .intent(intent)
                .objective(domainObjective)
                .outputNeeds(outputNeeds)
                .role(null)
                .actionType(null)
                .tone(command.tone())
                .style(command.style())
                .language(command.language())
                .experienceLevel(command.experience())
                .appliedProfileIds(List.of("request_mode:EXTRACTION"))
                .validationWarnings(List.of())
                .recommendationHints(List.of())
                .build();

        return Result.ok(axes);
    }

    public record Result(boolean success, ConfirmedSemanticAxes axes, List<String> errors) {
        public static Result ok(ConfirmedSemanticAxes axes) {
            return new Result(true, axes, List.of());
        }

        public static Result fail(List<String> errors) {
            return new Result(false, null, errors != null ? List.copyOf(errors) : List.of());
        }
    }
}
