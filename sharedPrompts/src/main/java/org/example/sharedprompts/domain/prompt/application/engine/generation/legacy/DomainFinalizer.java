package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.engine.domain.DomainResolutionService;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 최종 TaskDomain 결정 */
@Component
@RequiredArgsConstructor
public class DomainFinalizer {

    private final DomainResolutionService domainResolutionService;

    public FinalDomainDecision finalizeDomain(
            UnifiedGeneratePromptCommand command,
            IntentDefaults defaults,
            RoutingOverrides overrides
    ) {
        List<String> reasons = new ArrayList<>();

        TaskDomain baseDomain = chooseBaseDomain(command.category(), defaults);
        reasons.add("baseDomain:" + baseDomain.name());

        if (overrides.domainOverride() != null) {
            baseDomain = overrides.domainOverride();
            reasons.add("ruleOverrideDomain:" + overrides.domainOverride().name());
        }

        ResolvedDomain resolved = domainResolutionService.resolveForUnified(
                command.category(),
                defaults.domainAffinity()
        );

        TaskDomain finalDomain = baseDomain;
        if (resolved.domain() != null) {
            finalDomain = resolved.domain();
            reasons.add("resolver:" + (resolved.source() != null ? resolved.source() : "unknown"));
        }

        return new FinalDomainDecision(finalDomain, List.copyOf(reasons));
    }

    private TaskDomain chooseBaseDomain(PromptCategory category, IntentDefaults defaults) {
        if (defaults.domainAffinity() != null) {
            return defaults.domainAffinity();
        }
        if (category != null && category.getDefaultDomain() != null) {
            return category.getDefaultDomain();
        }
        return TaskDomain.PRACTICAL;
    }
}
