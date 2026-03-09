package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.service.orchestration.DomainResolutionService;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainFinalizerTest {

    private DomainResolutionService domainResolutionService() {
        return new DomainResolutionService(new DomainResolverPort() {
            @Override
            public ResolvedDomain resolveDomain(org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface actionType, PromptCategory promptCategory) {
                return new ResolvedDomain(TaskDomain.PRACTICAL, false);
            }

            @Override
            public ResolvedDomain resolveDomainWithFallback(org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface actionType, PromptCategory promptCategory) {
                return new ResolvedDomain(TaskDomain.TECHNICAL, false);
            }
        });
    }

    private UnifiedGeneratePromptCommand command(PromptCategory category) {
        return UnifiedGeneratePromptCommand.of(
                1L,
                org.example.sharedprompts.domain.prompt.common.enums.RequestMode.SIMPLE,
                category,
                ActionIntent.GENERATE,
                null,
                "input",
                null,
                EngineMode.AUTO,
                ToneType.NEUTRAL,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void should_use_intent_affinity_and_resolver_result() {
        DomainFinalizer finalizer = new DomainFinalizer(domainResolutionService());

        var resolutionDefaults = IntentDictionary.getResolutionDefaults(ActionIntent.GENERATE);
        IntentDefaults defaults = new IntentDefaults(
                ActionIntent.GENERATE,
                resolutionDefaults.defaultObjective(),
                resolutionDefaults.preferredOutputNeeds(),
                resolutionDefaults.defaultResponseShape(),
                TaskDomain.TECHNICAL,
                EngineProfile.QUALITY_PIPELINE
        );

        RoutingRuleEngine.RoutingOverrides overrides = RoutingRuleEngine.RoutingOverrides.empty();

        FinalDomainDecision decision = finalizer.finalizeDomain(
                command(PromptCategory.DEVELOPMENT),
                defaults,
                overrides
        );

        assertThat(decision.domain()).isEqualTo(TaskDomain.TECHNICAL);
        assertThat(decision.reasons()).isNotEmpty();
    }
}

