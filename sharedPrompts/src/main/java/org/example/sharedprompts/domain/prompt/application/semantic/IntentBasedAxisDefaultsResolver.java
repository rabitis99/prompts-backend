package org.example.sharedprompts.domain.prompt.application.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.springframework.stereotype.Component;

/** Intent·프로필·jsonSchema 기반 objective/outputNeeds/taskDomain 해석 (단일 책임) */
@Component
public class IntentBasedAxisDefaultsResolver {

    public PromptObjective resolveObjective(ActionIntent intent, boolean hasJsonSchema, boolean extractionRequestMode) {
        var defaults = IntentDictionary.getResolutionDefaults(intent);
        org.example.sharedprompts.domain.prompt.common.enums.PromptObjective apiObjective = defaults.defaultObjective();
        if (hasJsonSchema && (extractionRequestMode || intent == ActionIntent.EXTRACT)) {
            apiObjective = org.example.sharedprompts.domain.prompt.common.enums.PromptObjective.EXTRACTION;
        }
        return apiObjective.toDomainObjective();
    }

    public OutputNeeds resolveOutputNeeds(ActionIntent intent, boolean hasJsonSchema) {
        var defaults = IntentDictionary.getResolutionDefaults(intent);
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
