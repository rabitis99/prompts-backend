package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.creative.CreativeActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CodingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.EtcActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * TaskDomain / PromptObjective 해석 빈 설정.
 *
 * <p>모든 빈을 <b>인터페이스 타입</b>으로만 노출하여 주입 시 접근 오류를 방지.
 * {@link PromptDomainConfig}에서 import.
 */
@Configuration
public class ResolutionConfig {

    // Resolution policy: action-type → objective. Prefer registry over enum getters.
    // 나머지 ActionType은 레지스트리의 키워드 기반 추론 규칙을 통해 적절한 PromptObjective로 매핑된다.
    private static final Map<ActionTypeInterface, PromptObjective> EXPLICIT_MAPPINGS = Map.ofEntries(
            Map.entry(CodingActionType.CODE_REVIEW, PromptObjective.REASONING),
            Map.entry(CodingActionType.DEBUGGING, PromptObjective.REASONING),
            Map.entry(CodingActionType.REFACTORING, PromptObjective.REASONING),
            Map.entry(AnalysisActionType.DATA_ANALYSIS, PromptObjective.ANALYTICAL),
            Map.entry(AnalysisActionType.COMPARATIVE_ANALYSIS, PromptObjective.ANALYTICAL),
            Map.entry(AnalysisActionType.ROOT_CAUSE_ANALYSIS, PromptObjective.ANALYTICAL),
            Map.entry(CreativeActionType.CREATIVE_WRITING, PromptObjective.CREATIVE_WITH_CONSTRAINTS),
            Map.entry(CreativeActionType.IDEA_GENERATION, PromptObjective.CREATIVE_WITH_CONSTRAINTS),
            Map.entry(EtcActionType.PROBLEM_SOLVING, PromptObjective.REASONING),
            Map.entry(EtcActionType.EXPLANATION, PromptObjective.REASONING),
            Map.entry(WritingActionType.TRANSLATION, PromptObjective.FACTUAL),
            Map.entry(WritingActionType.PROOFREADING, PromptObjective.FACTUAL),
            Map.entry(ProductivityActionType.SCHEDULE_PLANNING, PromptObjective.PLANNING),
            Map.entry(ProductivityActionType.TASK_AUTOMATION, PromptObjective.PLANNING)
    );

    @Bean
    public DomainResolverPort domainResolver(ActionDomainRegistry actionDomainRegistry) {
        return new DomainResolver(actionDomainRegistry);
    }

    @Bean
    public ObjectiveMappingRegistryPort objectiveMappingRegistry(CanonicalActionRegistry canonicalActionRegistry) {
        ObjectiveMappingRegistry registry = new ObjectiveMappingRegistry(canonicalActionRegistry);
        EXPLICIT_MAPPINGS.forEach(registry::put);
        return registry;
    }

    @Bean
    public ObjectiveResolverPort objectiveResolver(ObjectiveMappingRegistryPort mappingRegistry) {
        return new ObjectiveResolver(mappingRegistry);
    }

    /**
     * Profile registry with group-first derivation: compatible actions are derived from
     * compatible ActionGroups via {@link ActionTypeRegistry}. Requires both registries.
     */
    @Bean
    public CategorySemanticProfileRegistry categorySemanticProfileRegistry(
            CanonicalActionRegistry canonicalActionRegistry,
            ActionTypeRegistry actionTypeRegistry) {
        return new DefaultCategorySemanticProfileRegistry(canonicalActionRegistry, actionTypeRegistry);
    }
}
