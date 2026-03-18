package org.example.sharedprompts.domain.prompt.infrastructure.policy.binding;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.ObjectivePolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Binds validated {@link ObjectivePolicyDocument} to runtime {@link ObjectivePolicySource}.
 */
public final class ObjectivePolicyBinder {

    public ObjectivePolicySource bind(ObjectivePolicyDocument document) {
        if (document == null) {
            return new DefaultObjectivePolicySource();
        }
        Map<String, PromptObjective> explicitByKey = new HashMap<>();
        for (var e : document.explicitMappings().entrySet()) {
            PromptObjective obj = parseObjective(e.getValue());
            if (obj != null) {
                explicitByKey.put(e.getKey(), obj);
            }
        }
        Map<TaskDomain, PromptObjective> domainDefaults = new EnumMap<>(TaskDomain.class);
        for (var e : document.domainDefaults().entrySet()) {
            TaskDomain domain = parseTaskDomain(e.getKey());
            PromptObjective obj = parseObjective(e.getValue());
            if (domain != null && obj != null) {
                domainDefaults.put(domain, obj);
            }
        }
        return new DefaultObjectivePolicySource(explicitByKey, domainDefaults.isEmpty() ? null : domainDefaults);
    }

    private static PromptObjective parseObjective(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return PromptObjective.valueOf(name.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static TaskDomain parseTaskDomain(String name) {
        if (name == null || name.isBlank()) return null;
        String n = name.trim();
        for (TaskDomain td : TaskDomain.values()) {
            if (td.name().equals(n) || (td.key() != null && td.key().equals(n))) {
                return td;
            }
        }
        return null;
    }
}
