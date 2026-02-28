package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.action.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.action.CodingActionType;
import org.example.sharedprompts.domain.prompt.enums.action.CreativeActionType;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.action.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.enums.action.WritingActionType;
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

    // 명시 매핑: ActionType.getDefaultObjective()가 null일 때 사용
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
    public DomainResolverPort domainResolver() {
        return new DomainResolver();
    }

    @Bean
    public ObjectiveMappingRegistryPort objectiveMappingRegistry() {
        ObjectiveMappingRegistry registry = new ObjectiveMappingRegistry();
        EXPLICIT_MAPPINGS.forEach(registry::put);
        return registry;
    }

    @Bean
    public ObjectiveResolverPort objectiveResolver(ObjectiveMappingRegistryPort mappingRegistry) {
        return new ObjectiveResolver(mappingRegistry);
    }
}
