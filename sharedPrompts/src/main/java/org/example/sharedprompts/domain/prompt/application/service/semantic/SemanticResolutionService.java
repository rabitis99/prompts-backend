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
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
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

        CoreResolutionResult core = performCoreResolution(
                command.category(),
                command.intent(),
                command.roleType(),
                command.actionType(),
                (profile, intent) -> validationService.validate(command, profile, intent)
        );

        if (!core.success()) {
            return Result.fail(core.errors());
        }

        ActionIntent intent = core.resolvedIntent();
        CategorySemanticProfile profile = core.profile();

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
                : command.category().getDefaultDomain();

        List<String> appliedIds = new ArrayList<>();
        appliedIds.add("profile:" + command.category().name());
        appliedIds.add("intent:" + intent.name());
        if (core.validation().severity() == SemanticValidationResult.Severity.WARNING) {
            appliedIds.add("validation:warnings");
        }

        ResolutionMetadata metadata = new ResolutionMetadata(
                core.fallbackIntentUsed(),
                command.intent() != null,
                command.roleType() != null,
                command.actionType() != null,
                false
        );

        ConfirmedSemanticAxes axes = ConfirmedSemanticAxes.builder()
                .category(command.category())
                .taskDomain(taskDomain)
                .intent(intent)
                .objective(domainObjective)
                .outputNeeds(outputNeeds)
                .role(core.recommendation().recommendedRole().orElse(null))
                .actionType(core.recommendation().recommendedAction().orElse(null))
                .tone(command.tone())
                .style(command.style())
                .language(command.language())
                .experienceLevel(command.experience())
                .appliedProfileIds(appliedIds)
                .validationWarnings(core.warnings())
                .recommendationHints(core.recommendation().recommendationHints())
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
            boolean userProvidedAction,
            boolean isExtraction
    ) {
        public static ResolutionMetadata forExtraction() {
            return new ResolutionMetadata(false, false, false, false, true);
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

    /** Extraction: intent is implied by request mode, role/action are not applicable. */
    private Map<String, String> buildAxisSourcesForExtraction() {
        return Map.of(
                "intent", AxisSourceConstants.IMPLIED_BY_MODE,
                "objective", AxisSourceConstants.IMPLIED_BY_MODE,
                "output_needs", AxisSourceConstants.IMPLIED_BY_MODE
        );
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

        CoreResolutionResult core = performCoreResolution(
                command.category(),
                command.intent(),
                command.roleType(),
                command.actionType(),
                (profile, intent) -> validationService.validate(command, profile, intent)
        );

        if (!core.success()) {
            throw new SemanticResolutionException(core.errors());
        }

        ActionIntent intent = core.resolvedIntent();
        CategorySemanticProfile profile = core.profile();

        Map<String, String> axisSources = buildAxisSources(
                command.intent() != null,
                command.roleType() != null,
                command.actionType() != null,
                core.fallbackIntentUsed()
        );

        List<String> fallbackApplied = new ArrayList<>();
        if (core.fallbackIntentUsed()) {
            fallbackApplied.add("intent: profile fallback applied");
        }

        List<ActionIntent> intentCandidates = profile != null
                ? new ArrayList<>(profile.getAllowedIntents())
                : List.of();

        return new RecommendPromptResult(
                command.requestMode(),
                command.category(),
                intent,
                intentCandidates,
                core.recommendation().recommendedRole().orElse(null),
                core.recommendation().roleCandidates(),
                core.recommendation().recommendedAction().orElse(null),
                core.recommendation().actionCandidates(),
                command.tone(),
                command.style(),
                axisSources,
                core.recommendation().recommendationHints(),
                core.warnings(),
                fallbackApplied,
                intent.name()
        );
    }

    private record CoreResolutionResult(
            boolean success,
            List<String> errors,
            ActionIntent resolvedIntent,
            CategorySemanticProfile profile,
            boolean fallbackIntentUsed,
            SemanticValidationResult validation,
            RecommendationResult recommendation,
            List<String> warnings
    ) {
        static CoreResolutionResult fail(List<String> errors) {
            return new CoreResolutionResult(false, errors, null, null, false, null, null, null);
        }
        static CoreResolutionResult ok(
                ActionIntent resolvedIntent,
                CategorySemanticProfile profile,
                boolean fallbackIntentUsed,
                SemanticValidationResult validation,
                RecommendationResult recommendation,
                List<String> warnings) {
            return new CoreResolutionResult(true, null, resolvedIntent, profile, fallbackIntentUsed, validation, recommendation, warnings);
        }
    }

    private CoreResolutionResult performCoreResolution(
            PromptCategory category,
            ActionIntent initialIntent,
            org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface roleType,
            org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface actionType,
            java.util.function.BiFunction<CategorySemanticProfile, ActionIntent, SemanticValidationResult> validator
    ) {
        if (category == null) {
            return CoreResolutionResult.fail(List.of("category is required for SIMPLE/ADVANCED"));
        }
        if (category == PromptCategory.EXTRACTION) {
            return CoreResolutionResult.fail(List.of("EXTRACTION category is only valid with request_mode=EXTRACTION"));
        }

        boolean fallbackIntentUsed = false;
        ActionIntent intent = initialIntent;
        if (intent == null) {
            Optional<CategorySemanticProfile> profileForFallbackOpt = profileRegistry.getProfile(category);
            if (profileForFallbackOpt.map(p -> p.getFallbackIntent() != null).orElse(false)) {
                intent = profileForFallbackOpt.get().getFallbackIntent();
                fallbackIntentUsed = true;
            } else {
                return CoreResolutionResult.fail(List.of("intent is required for SIMPLE/ADVANCED"));
            }
        }

        CategorySemanticProfile profile = profileRegistry.getProfile(category).orElse(null);

        SemanticValidationResult validation = validator.apply(profile, intent);
        if (validation.severity() == SemanticValidationResult.Severity.ERROR) {
            List<String> messages = validation.items().stream()
                    .map(i -> i.code() + ": " + i.message())
                    .toList();
            return CoreResolutionResult.fail(messages);
        }

        var recommendation = recommendationService.recommend(
                category,
                intent,
                profile,
                roleType,
                actionType,
                fallbackIntentUsed
        );

        List<String> warnings = new ArrayList<>();
        if (validation.severity() == SemanticValidationResult.Severity.WARNING) {
            validation.items().forEach(i -> warnings.add(i.message()));
        }

        return CoreResolutionResult.ok(intent, profile, fallbackIntentUsed, validation, recommendation, warnings);
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
