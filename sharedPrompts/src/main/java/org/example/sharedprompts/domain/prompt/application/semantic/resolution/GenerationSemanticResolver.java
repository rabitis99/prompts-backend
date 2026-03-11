package org.example.sharedprompts.domain.prompt.application.semantic.resolution;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.semantic.IntentBasedAxisDefaultsResolver;
import org.example.sharedprompts.domain.prompt.application.semantic.validation.SemanticValidationService;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 생성 플로우: Command → ConfirmedSemanticAxes + 메타데이터 */
@Component
public class GenerationSemanticResolver {

    private final CoreSemanticResolver coreSemanticResolver;
    private final SemanticValidationService validationService;
    private final IntentBasedAxisDefaultsResolver axisDefaultsResolver;

    public GenerationSemanticResolver(
            CoreSemanticResolver coreSemanticResolver,
            SemanticValidationService validationService,
            IntentBasedAxisDefaultsResolver axisDefaultsResolver
    ) {
        this.coreSemanticResolver = coreSemanticResolver;
        this.validationService = validationService;
        this.axisDefaultsResolver = axisDefaultsResolver;
    }

    public ResolutionResult.Result resolve(UnifiedGeneratePromptCommand command) {
        if (command.requestMode() == RequestMode.EXTRACTION) {
            return resolveExtraction(command);
        }

        CoreSemanticResolver.CoreResolutionResult core = coreSemanticResolver.performCoreResolution(
                command.requestMode(),
                command.category(),
                command.intent(),
                command.roleType(),
                command.actionType(),
                (profile, intent) -> validationService.validate(command, profile, intent)
        );

        if (!core.success()) {
            return ResolutionResult.Result.fail(core.errors());
        }

        ActionIntent intent = core.resolvedIntent();
        CategorySemanticProfile profile = core.profile();
        boolean hasJsonSchema = command.jsonSchema() != null && !command.jsonSchema().isBlank();

        PromptObjective domainObjective = axisDefaultsResolver.resolveObjective(intent, hasJsonSchema, false);
        OutputNeeds outputNeeds = axisDefaultsResolver.resolveOutputNeeds(intent, hasJsonSchema);
        TaskDomain taskDomain = axisDefaultsResolver.resolveTaskDomain(profile, command.category());

        List<String> appliedIds = new ArrayList<>();
        appliedIds.add("profile:" + command.category().name());
        appliedIds.add("intent:" + intent.name());
        if (core.validation().severity() == SemanticValidationResult.Severity.WARNING) {
            appliedIds.add("validation:warnings");
        }

        ResolutionResult.ResolutionMetadata metadata = new ResolutionResult.ResolutionMetadata(
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

        return ResolutionResult.Result.ok(axes, metadata);
    }

    private ResolutionResult.Result resolveExtraction(UnifiedGeneratePromptCommand command) {
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

        return ResolutionResult.Result.ok(axes, ResolutionResult.ResolutionMetadata.forExtraction());
    }
}
