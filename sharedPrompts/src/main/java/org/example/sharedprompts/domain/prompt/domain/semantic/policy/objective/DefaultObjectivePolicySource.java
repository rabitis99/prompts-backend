package org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Default in-memory objective policy source.
 * Replaceable by classpath/json/yaml/DB implementation without changing service code.
 * Built-in explicit mappings ({@code DEFAULT_EXPLICIT_MAPPINGS}) and domain defaults are
 * owned by this implementation; config does not hold raw maps. Swap with another
 * {@link ObjectivePolicySource} for custom mappings.
 */
public final class DefaultObjectivePolicySource implements ObjectivePolicySource {

    private static final Map<String, PromptObjective> DEFAULT_EXPLICIT_MAPPINGS = Map.ofEntries(
            Map.entry("ACTION.CODING.CODE_REVIEW", PromptObjective.REASONING),
            Map.entry("ACTION.CODING.DEBUGGING", PromptObjective.REASONING),
            Map.entry("ACTION.CODING.REFACTORING", PromptObjective.REASONING),
            Map.entry("ACTION.ANALYSIS.DATA_ANALYSIS", PromptObjective.ANALYTICAL),
            Map.entry("ACTION.ANALYSIS.COMPARATIVE_ANALYSIS", PromptObjective.ANALYTICAL),
            Map.entry("ACTION.ANALYSIS.ROOT_CAUSE_ANALYSIS", PromptObjective.ANALYTICAL),
            Map.entry("ACTION.CREATIVE.CREATIVE_WRITING", PromptObjective.CREATIVE_WITH_CONSTRAINTS),
            Map.entry("ACTION.CREATIVE.IDEA_GENERATION", PromptObjective.CREATIVE_WITH_CONSTRAINTS),
            Map.entry("ACTION.ETC.PROBLEM_SOLVING", PromptObjective.REASONING),
            Map.entry("ACTION.ETC.EXPLANATION", PromptObjective.REASONING),
            Map.entry("ACTION.WRITING.TRANSLATION", PromptObjective.FACTUAL),
            Map.entry("ACTION.WRITING.PROOFREADING", PromptObjective.FACTUAL),
            Map.entry("ACTION.PRODUCTIVITY.SCHEDULE_PLANNING", PromptObjective.PLANNING),
            Map.entry("ACTION.PRODUCTIVITY.TASK_AUTOMATION", PromptObjective.PLANNING)
    );

    private final Map<String, PromptObjective> explicitByKey;
    private final Map<TaskDomain, PromptObjective> domainDefaults;

    /** Production default: built-in explicit mappings and standard domain defaults. */
    public DefaultObjectivePolicySource() {
        this.explicitByKey = Map.copyOf(DEFAULT_EXPLICIT_MAPPINGS);
        this.domainDefaults = defaultDomainDefaults();
    }

    /** Custom explicit map; domain defaults as standard. */
    public DefaultObjectivePolicySource(Map<String, PromptObjective> explicitByKey) {
        this.explicitByKey = explicitByKey != null ? Map.copyOf(explicitByKey) : Map.of();
        this.domainDefaults = defaultDomainDefaults();
    }

    /** Full customisation for tests or alternate policies. */
    public DefaultObjectivePolicySource(
            Map<String, PromptObjective> explicitByKey,
            Map<TaskDomain, PromptObjective> domainDefaults
    ) {
        this.explicitByKey = explicitByKey != null ? Map.copyOf(explicitByKey) : Map.of();
        this.domainDefaults = domainDefaults != null ? Map.copyOf(domainDefaults) : defaultDomainDefaults();
    }

    private static Map<TaskDomain, PromptObjective> defaultDomainDefaults() {
        return Map.of(
                TaskDomain.TECHNICAL, PromptObjective.REASONING,
                TaskDomain.EDUCATIONAL, PromptObjective.REASONING,
                TaskDomain.GENERAL, PromptObjective.REASONING,
                TaskDomain.ANALYTICAL, PromptObjective.ANALYTICAL,
                TaskDomain.CREATIVE, PromptObjective.CREATIVE_WITH_CONSTRAINTS,
                TaskDomain.PRACTICAL, PromptObjective.PLANNING
        );
    }

    @Override
    public Optional<PromptObjective> findByStableKey(String actionStableKey) {
        if (actionStableKey == null || actionStableKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(explicitByKey.get(actionStableKey.trim()));
    }

    @Override
    public PromptObjective getDomainDefault(TaskDomain taskDomain) {
        if (taskDomain == null) {
            return PromptObjective.REASONING;
        }
        return domainDefaults.getOrDefault(taskDomain, PromptObjective.REASONING);
    }
}
