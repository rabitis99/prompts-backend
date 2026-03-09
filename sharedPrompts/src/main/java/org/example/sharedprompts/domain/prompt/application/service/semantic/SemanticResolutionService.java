package org.example.sharedprompts.domain.prompt.application.service.semantic;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
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
import org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException;
import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves command to confirmed semantic axes using profile → recommend → validate.
 * {@link RequestMode#EXTRACTION} is handled separately and yields {@link PromptCategory#EXTRACTION};
 * SIMPLE/ADVANCED require category and intent (or profile fallback) and return a {@link Result}
 * (success with axes or failure with errors).
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
     * Uses {@link RequestMode} from the command: EXTRACTION forces intent=EXTRACT and category=EXTRACTION;
     * SIMPLE/ADVANCED require category and intent (or profile fallback).
     *
     * @param command the unified command (requestMode, category, intent, etc.)
     * @return {@link Result} with success and axes, or failure with error messages (never null)
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
        if (category == PromptCategory.EXTRACTION) {
            return Result.fail(List.of("EXTRACTION category is only valid with requestMode=EXTRACTION; use request_type=EXTRACTION for extraction requests"));
        }

        boolean fallbackIntentUsed = false;
        if (intent == null) {
            Optional<CategorySemanticProfile> profileForFallbackOpt = profileRegistry.getProfile(category);
            if (profileForFallbackOpt.map(p -> p.getFallbackIntent() != null).orElse(false)) {
                intent = profileForFallbackOpt.get().getFallbackIntent();
                fallbackIntentUsed = true;
            } else {
                return Result.fail(List.of("intent is required for SIMPLE/ADVANCED"));
            }
        }

        CategorySemanticProfile profile = profileRegistry.getProfile(category).orElse(null);
        SemanticValidationResult validation = validationService.validate(command, profile, intent);
        if (validation.severity() == SemanticValidationResult.Severity.ERROR) {
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
            outputNeeds = OutputNeeds.JSON_SCHEMA_REQUIRED;
            if (command.requestMode() == RequestMode.EXTRACTION || intent == ActionIntent.EXTRACT) {
                apiObjective = org.example.sharedprompts.domain.prompt.common.enums.PromptObjective.EXTRACTION;
            }
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

        ResolutionMetadata metadata = new ResolutionMetadata(
                fallbackIntentUsed,
                command.intent() != null,
                command.roleType() != null,
                command.actionType() != null
        );

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

        return Result.ok(axes, metadata);
    }

    /**
     * Metadata for axis_sources assembly. Orchestrator uses this to build the final axis_sources map
     * (document: "Populate sources inside UnifiedPromptGenerationOrchestrator after semantic resolution").
     */
    public record ResolutionMetadata(
            boolean fallbackIntentUsed,
            boolean userProvidedIntent,
            boolean userProvidedRole,
            boolean userProvidedAction
    ) {
        public static ResolutionMetadata forExtraction() {
            return new ResolutionMetadata(false, true, false, false);
        }
    }

    private Map<String, String> buildAxisSources(
            boolean intentUserProvided,
            boolean roleUserProvided,
            boolean actionUserProvided,
            boolean fallbackIntentUsed
    ) {
        return Map.of(
                "intent", intentUserProvided ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.FALLBACK,
                "role", roleUserProvided ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.RECOMMENDED,
                "action", actionUserProvided ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.RECOMMENDED,
                "objective", AxisSourceConstants.RECOMMENDED,
                "output_needs", AxisSourceConstants.RECOMMENDED
        );
    }

    /** Extraction: intent is explicit (user-provided), role/action recommended. Reuses same rules as buildAxisSources. */
    private Map<String, String> buildAxisSourcesForExtraction() {
        return buildAxisSources(true, false, false, false);
    }

    private Result resolveExtraction(UnifiedGeneratePromptCommand command) {
        ActionIntent intent = ActionIntent.EXTRACT;
        TaskDomain taskDomain = TaskDomain.ANALYTICAL;
        PromptObjective domainObjective = PromptObjective.EXTRACTION;
        OutputNeeds outputNeeds = OutputNeeds.JSON_SCHEMA_REQUIRED;

        ConfirmedSemanticAxes axes = ConfirmedSemanticAxes.builder()
                .category(PromptCategory.EXTRACTION)
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

        return Result.ok(axes, ResolutionMetadata.forExtraction());
    }

    /**
     * Recommendation-only resolution: returns recommended axes and candidates without building ConfirmedSemanticAxes or triggering generation.
     * EXTRACTION is delegated to {@link #resolveForRecommendationExtraction(RecommendPromptCommand)}; SIMPLE/ADVANCED share validation and recommendation flow.
     * axis_sources use the same rule constants (USER_PROVIDED, RECOMMENDED, FALLBACK) in both paths for consistency.
     *
     * @param command the recommendation command (requestMode, category, optional intent/role/action, tone, style, etc.)
     * @return recommendation view (recommended intent, role, action, candidates, axis sources, hints, warnings)
     * @throws org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException on validation failure
     */
    public RecommendPromptResult resolveForRecommendation(RecommendPromptCommand command) {
        if (command.requestMode() == RequestMode.EXTRACTION) {
            return resolveForRecommendationExtraction(command);
        }

        PromptCategory category = command.category();
        ActionIntent intent = command.intent();

        if (category == null) {
            throw new SemanticResolutionException(
                    List.of("category is required for SIMPLE/ADVANCED"));
        }
        if (category == PromptCategory.EXTRACTION) {
            throw new SemanticResolutionException(
                    List.of("EXTRACTION category is only valid with request_mode=EXTRACTION"));
        }

        boolean fallbackIntentUsed = false;
        if (intent == null) {
            Optional<CategorySemanticProfile> profileForFallbackOpt = profileRegistry.getProfile(category);
            if (profileForFallbackOpt.map(p -> p.getFallbackIntent() != null).orElse(false)) {
                intent = profileForFallbackOpt.get().getFallbackIntent();
                fallbackIntentUsed = true;
            } else {
                throw new SemanticResolutionException(
                        List.of("intent is required for SIMPLE/ADVANCED"));
            }
        }

        CategorySemanticProfile profile = profileRegistry.getProfile(category).orElse(null);
        SemanticValidationResult validation = validationService.validate(command, profile, intent);
        if (validation.severity() == SemanticValidationResult.Severity.ERROR) {
            List<String> messages = validation.items().stream()
                    .map(i -> i.code() + ": " + i.message())
                    .toList();
            throw new SemanticResolutionException(messages);
        }

        var recommendation = recommendationService.recommend(
                category,
                intent,
                profile,
                command.roleType(),
                command.actionType(),
                fallbackIntentUsed
        );

        Map<String, String> axisSources = buildAxisSources(
                command.intent() != null,
                command.roleType() != null,
                command.actionType() != null,
                fallbackIntentUsed
        );

        List<String> warnings = new ArrayList<>();
        if (validation.severity() == SemanticValidationResult.Severity.WARNING) {
            validation.items().forEach(i -> warnings.add(i.message()));
        }

        List<String> fallbackApplied = new ArrayList<>();
        if (fallbackIntentUsed) {
            fallbackApplied.add("intent: profile fallback applied");
        }

        List<ActionIntent> intentCandidates = profile != null
                ? new ArrayList<>(profile.getAllowedIntents())
                : List.of();

        return new RecommendPromptResult(
                command.requestMode(),
                category,
                intent,
                intentCandidates,
                recommendation.recommendedRole().orElse(null),
                recommendation.roleCandidates(),
                recommendation.recommendedAction().orElse(null),
                recommendation.actionCandidates(),
                command.tone(),
                command.style(),
                axisSources,
                recommendation.recommendationHints(),
                warnings,
                fallbackApplied,
                intent.name()
        );
    }

    private RecommendPromptResult resolveForRecommendationExtraction(RecommendPromptCommand command) {
        return new RecommendPromptResult(
                RequestMode.EXTRACTION,
                PromptCategory.EXTRACTION,
                ActionIntent.EXTRACT,
                List.of(ActionIntent.EXTRACT),
                null,
                List.of(),
                null,
                List.of(),
                command.tone(),
                command.style(),
                buildAxisSourcesForExtraction(),
                List.of(),
                List.of(),
                List.of(),
                ActionIntent.EXTRACT.name()
        );
    }

    public record Result(boolean success, ConfirmedSemanticAxes axes, List<String> errors, ResolutionMetadata metadata) {
        public static Result ok(ConfirmedSemanticAxes axes, ResolutionMetadata metadata) {
            return new Result(true, axes, List.of(), metadata);
        }

        public static Result fail(List<String> errors) {
            return new Result(false, null, errors != null ? List.copyOf(errors) : List.of(), null);
        }
    }
}
