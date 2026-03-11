package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Objects;

/** 룰 매칭 조건: intent, category, jsonSchema 유무, domainAffinity */
@Getter
@RequiredArgsConstructor
@ToString
public final class RoutingCondition {

    private final String intentName;
    private final String categoryKey;
    private final Boolean hasJsonSchema;
    private final TaskDomain domainAffinity;

    public int specificity() {
        int count = 0;
        if (intentName != null) count++;
        if (categoryKey != null) count++;
        if (hasJsonSchema != null) count++;
        if (domainAffinity != null) count++;
        return count;
    }

    public boolean matches(UnifiedGeneratePromptCommand command, IntentDefaults defaults) {
        if (intentName != null && (command.intent() == null
                || !intentName.equals(command.intent().name()))) {
            return false;
        }
        if (categoryKey != null && (command.category() == null
                || !categoryKey.equals(command.category().key()))) {
            return false;
        }
        if (hasJsonSchema != null) {
            boolean commandHasSchema = command.jsonSchema() != null && !command.jsonSchema().isBlank();
            if (!Objects.equals(hasJsonSchema, commandHasSchema)) {
                return false;
            }
        }
        if (domainAffinity != null && !Objects.equals(domainAffinity, defaults.domainAffinity())) {
            return false;
        }
        return true;
    }

    public static RoutingCondition empty() {
        return new RoutingCondition(null, null, null, null);
    }
}
