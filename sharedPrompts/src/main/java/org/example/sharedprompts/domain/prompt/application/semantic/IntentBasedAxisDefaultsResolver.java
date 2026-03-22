package org.example.sharedprompts.domain.prompt.application.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Intent·프로필·jsonSchema 기반 objective/outputNeeds/taskDomain 해석 (단일 책임) */
@Component
public class IntentBasedAxisDefaultsResolver {

    private final IntentDictionary intentDictionary;

    public IntentBasedAxisDefaultsResolver(IntentDictionary intentDictionary) {
        this.intentDictionary = Objects.requireNonNull(intentDictionary, "intentDictionary");
    }

    public PromptObjective resolveObjective(ActionIntent intent, boolean hasJsonSchema, boolean extractionRequestMode) {
        var defaults = intentDictionary.getResolutionDefaults(intent);
        org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective apiObjective = defaults.defaultObjective();
        if (hasJsonSchema && (extractionRequestMode || intent == ActionIntent.EXTRACT)) {
            apiObjective = org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective.EXTRACTION;
        }
        return apiObjective.toDomainObjective();
    }

    public OutputNeeds resolveOutputNeeds(ActionIntent intent, boolean hasJsonSchema) {
        var defaults = intentDictionary.getResolutionDefaults(intent);
        if (hasJsonSchema) {
            return OutputNeeds.JSON_SCHEMA_REQUIRED;
        }
        return defaults.preferredOutputNeeds();
    }

    public TaskDomain resolveTaskDomain(
            CategorySemanticProfile profile,
            PromptCategory category
    ) {
        if (profile != null) {
            return profile.getBaseTaskDomain();
        }
        return category != null ? category.getDefaultDomain() : null;
    }
}
