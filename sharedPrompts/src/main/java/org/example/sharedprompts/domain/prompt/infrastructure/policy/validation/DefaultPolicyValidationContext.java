package org.example.sharedprompts.domain.prompt.infrastructure.policy.validation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Policy validation context built from ActionTypeRegistry, CanonicalActionRegistry, and enums.
 * Role keys and task domain names can be supplied optionally (e.g. from config).
 */
public final class DefaultPolicyValidationContext implements PolicyValidationContext {

    private final Set<String> categoryKeys;
    private final Set<String> intentKeys;
    private final Set<String> actionKeys;
    private final Set<String> actionGroupKeys;
    private final Set<String> roleKeys;
    private final Set<String> objectiveNames;
    private final Set<String> taskDomainNames;

    public DefaultPolicyValidationContext(
            ActionTypeRegistry actionTypeRegistry,
            CanonicalActionRegistry canonicalActionRegistry,
            Set<String> roleKeys,
            Set<String> taskDomainNames
    ) {
        this.categoryKeys = Arrays.stream(PromptCategory.values())
                .flatMap(c -> Stream.of(c.getKey(), c.name()))
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.intentKeys = Arrays.stream(ActionIntent.values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        this.actionKeys = actionTypeRegistry != null
                ? actionTypeRegistry.getAll().stream()
                .map(ActionTypeInterface::key)
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toUnmodifiableSet())
                : Set.of();
        this.actionGroupKeys = canonicalActionRegistry != null
                ? Stream.of(ActionGroup.values()).map(Enum::name).collect(Collectors.toUnmodifiableSet())
                : Set.of();
        this.roleKeys = roleKeys != null ? Set.copyOf(roleKeys) : Set.of();
        this.objectiveNames = Arrays.stream(PromptObjective.values())
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        this.taskDomainNames = taskDomainNames != null && !taskDomainNames.isEmpty()
                ? Set.copyOf(taskDomainNames)
                : Arrays.stream(org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain.values())
                .flatMap(td -> Stream.of(td.name(), td.key()))
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> knownCategoryKeys() {
        return categoryKeys;
    }

    @Override
    public Set<String> knownIntentKeys() {
        return intentKeys;
    }

    @Override
    public Set<String> knownActionKeys() {
        return actionKeys;
    }

    @Override
    public Set<String> knownActionGroupKeys() {
        return actionGroupKeys;
    }

    @Override
    public Set<String> knownRoleKeys() {
        return roleKeys;
    }

    @Override
    public Set<String> knownObjectiveNames() {
        return objectiveNames;
    }

    @Override
    public Set<String> knownTaskDomainNames() {
        return taskDomainNames;
    }
}
