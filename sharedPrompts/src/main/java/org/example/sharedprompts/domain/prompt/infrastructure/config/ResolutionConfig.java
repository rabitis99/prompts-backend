package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.domain.resolution.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.resolution.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolution.ExplicitObjectiveMapping;
import org.example.sharedprompts.domain.prompt.domain.resolution.ExplicitObjectiveMappingPort;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveMappingRegistryPort;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveResolverPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TaskDomain / PromptObjective 해석 전용 빈 설정.
 *
 * <p>해석 관련 빈만 두어 단일 책임 유지. {@link PromptDomainConfig}에서 import.
 * 포트(인터페이스) 타입으로 노출하여 DIP 유지.
 */
@Configuration
public class ResolutionConfig {

    @Bean
    public DomainResolverPort domainResolver() {
        return new DomainResolver();
    }

    @Bean
    public ObjectiveMappingRegistryPort objectiveMappingRegistry() {
        return new ObjectiveMappingRegistry();
    }

    @Bean
    public ExplicitObjectiveMapping explicitObjectiveMapping() {
        ExplicitObjectiveMapping mapping = new ExplicitObjectiveMapping();
        // 필요 시 명시 매핑: mapping.put(SomeActionType.EXTRACT, PromptObjective.EXTRACTION);
        return mapping;
    }

    @Bean
    public ObjectiveResolverPort objectiveResolver(ExplicitObjectiveMappingPort explicitObjectiveMapping,
                                                   ObjectiveMappingRegistryPort objectiveMappingRegistry) {
        return new ObjectiveResolver(explicitObjectiveMapping, objectiveMappingRegistry);
    }
}
