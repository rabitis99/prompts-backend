package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.action.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.enums.action.CodingActionType;
import org.example.sharedprompts.domain.prompt.enums.action.CreativeActionType;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.action.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.enums.action.WritingActionType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TaskDomain / PromptObjective 해석 빈 설정.
 *
 * <p>모든 빈을 <b>인터페이스 타입</b>으로만 노출하여 주입 시 접근 오류를 방지.
 * {@link PromptDomainConfig}에서 import.
 */
@Configuration
public class ResolutionConfig {

    @Bean
    public DomainResolverPort domainResolver() {
        return new DomainResolver();
    }

    @Bean
    public ObjectiveMappingRegistryPort objectiveMappingRegistry() {
        ObjectiveMappingRegistry registry = new ObjectiveMappingRegistry();
        // 명시 매핑: ActionType.getDefaultObjective()가 null일 때 사용
        registry.put(CodingActionType.CODE_REVIEW, PromptObjective.REASONING);
        registry.put(CodingActionType.DEBUGGING, PromptObjective.REASONING);
        registry.put(CodingActionType.REFACTORING, PromptObjective.REASONING);
        registry.put(AnalysisActionType.DATA_ANALYSIS, PromptObjective.ANALYTICAL);
        registry.put(AnalysisActionType.COMPARATIVE_ANALYSIS, PromptObjective.ANALYTICAL);
        registry.put(AnalysisActionType.ROOT_CAUSE_ANALYSIS, PromptObjective.ANALYTICAL);
        registry.put(CreativeActionType.CREATIVE_WRITING, PromptObjective.CREATIVE_WITH_CONSTRAINTS);
        registry.put(CreativeActionType.IDEA_GENERATION, PromptObjective.CREATIVE_WITH_CONSTRAINTS);
        registry.put(EtcActionType.PROBLEM_SOLVING, PromptObjective.REASONING);
        registry.put(EtcActionType.EXPLANATION, PromptObjective.REASONING);
        registry.put(WritingActionType.TRANSLATION, PromptObjective.FACTUAL);
        registry.put(WritingActionType.PROOFREADING, PromptObjective.FACTUAL);
        registry.put(ProductivityActionType.SCHEDULE_PLANNING, PromptObjective.PLANNING);
        registry.put(ProductivityActionType.TASK_AUTOMATION, PromptObjective.PLANNING);
        return registry;
    }

    @Bean
    public ObjectiveResolverPort objectiveResolver(ObjectiveMappingRegistryPort objectiveMappingRegistry) {
        return new ObjectiveResolver(objectiveMappingRegistry);
    }
}
