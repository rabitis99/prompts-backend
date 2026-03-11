package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.engine.domain.DomainResolutionService;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.role.core.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.metadata.DomainRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ResolvedDomain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedRoutingFacadeTest {

    private DomainResolutionService domainResolutionService(TaskDomain resultDomain) {
        return new DomainResolutionService(new DomainResolverPort() {
            @Override
            public ResolvedDomain resolveDomain(org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface actionType, PromptCategory promptCategory) {
                return new ResolvedDomain(resultDomain, false);
            }

            @Override
            public ResolvedDomain resolveDomainWithFallback(org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface actionType, PromptCategory promptCategory) {
                return new ResolvedDomain(resultDomain, false);
            }
        });
    }

    private UnifiedGeneratePromptCommand baseCommand(EngineMode mode, String jsonSchema) {
        return UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                PromptCategory.ANALYSIS,
                ActionIntent.SUMMARIZE,
                null,
                "input",
                jsonSchema,
                mode,
                ToneType.NEUTRAL,
                null,
                null,
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
    void auto_mode_without_schema_should_choose_quality_pipeline_and_v2() {
        UnifiedRoutingFacade facade = new UnifiedRoutingFacade(
                new IntentDefaultsResolver(),
                new RoutingRuleEngine(),
                new DomainFinalizer(domainResolutionService(TaskDomain.ANALYTICAL)),
                new OutputContractPlanner()
        );

        UnifiedRoutingFacade.RoutingDecision decision = facade.decide(baseCommand(EngineMode.AUTO, null));

        assertThat(decision.engineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
        assertThat(decision.effectiveEngineMode()).isEqualTo(EngineMode.V2);
        assertThat(decision.decisionReasons())
                .anySatisfy(r -> assertThat(r).contains("engineProfile:autoByIntent"));
    }

    @Test
    void json_schema_should_force_json_strict_profile_and_extraction_objective() {
        UnifiedRoutingFacade facade = new UnifiedRoutingFacade(
                new IntentDefaultsResolver(),
                new RoutingRuleEngine(),
                new DomainFinalizer(domainResolutionService(TaskDomain.ANALYTICAL)),
                new OutputContractPlanner()
        );

        UnifiedRoutingFacade.RoutingDecision decision = facade.decide(baseCommand(EngineMode.AUTO, "{}"));

        assertThat(decision.engineProfile()).isEqualTo(EngineProfile.JSON_STRICT);
        assertThat(decision.objective()).isEqualTo(PromptObjective.EXTRACTION);
        assertThat(decision.outputNeeds()).isEqualTo(OutputNeeds.JSON_SCHEMA_REQUIRED);
        assertThat(decision.decisionReasons())
                .anySatisfy(r -> assertThat(r).contains("jsonSchema"));
    }

    @Test
    void applied_rule_ids_and_reasons_should_be_propagated() {
        RoutingRuleEngine engine = new RoutingRuleEngine();

        engine.registerRule(new RoutingRule(
                "r1",
                1,
                new RoutingCondition("SUMMARIZE", null, null, null),
                null,
                OutputNeeds.STRUCTURED_TEXT,
                null,
                null,
                EngineProfile.QUALITY_PIPELINE,
                null,
                null
        ));

        UnifiedRoutingFacade facade = new UnifiedRoutingFacade(
                new IntentDefaultsResolver(),
                engine,
                new DomainFinalizer(domainResolutionService(TaskDomain.ANALYTICAL)),
                new OutputContractPlanner()
        );

        UnifiedRoutingFacade.RoutingDecision decision = facade.decide(baseCommand(EngineMode.AUTO, null));

        assertThat(decision.appliedRuleIds()).containsExactly("r1");
        assertThat(decision.decisionReasons())
                .anySatisfy(r -> assertThat(r).contains("rulesApplied"))
                .anySatisfy(r -> assertThat(r).contains("engineProfile"));
    }
}
