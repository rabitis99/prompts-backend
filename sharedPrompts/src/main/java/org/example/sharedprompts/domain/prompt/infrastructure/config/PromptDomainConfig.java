package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.domain.objective.registry.DefaultObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.AnalyticalObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.CreativeObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ExtractionObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.FactualObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.PlanningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ReasoningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.policy.strategy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.service.badge.BadgeResolver;
import org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecFactory;
import org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecValidator;
import org.example.sharedprompts.domain.prompt.domain.service.recommendation.RecommendationRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * 프롬프트 도메인 빈 설정.
 *
 * <p>해석 빈은 {@link ResolutionConfig}에서 정의. 새 Objective 추가 시
 * {@link #objectiveRegistry()} 의 프로파일 목록에만 추가하면 된다 (OCP).
 */
@Configuration
@Import(ResolutionConfig.class)
public class PromptDomainConfig {

    @Bean
    public ObjectiveRegistry objectiveRegistry() {
        // DefaultObjectiveRegistry 생성자에서 validate() 호출 — 모든 PromptObjective에 프로파일 등록 여부 검증 (등록 누락 시 기동 시점 IllegalStateException)
        return new DefaultObjectiveRegistry(List.of(
                new FactualObjectiveProfile(),
                new ReasoningObjectiveProfile(),
                new ExtractionObjectiveProfile(),
                new PlanningObjectiveProfile(),
                new CreativeObjectiveProfile(),
                new AnalyticalObjectiveProfile()
                // 새 Objective: new InstructionalObjectiveProfile() 한 줄만 추가
        ));
    }

    @Bean
    public StrategyBundlePolicy strategyBundlePolicy(ObjectiveRegistry objectiveRegistry) {
        return new StrategyBundlePolicy(objectiveRegistry);
    }

    @Bean
    public PromptSpecFactory promptSpecFactory(ObjectiveRegistry objectiveRegistry,
                                               StrategyBundlePolicy strategyBundlePolicy,
                                               ObjectiveResolverPort objectiveResolver) {
        return new PromptSpecFactory(objectiveRegistry, strategyBundlePolicy, objectiveResolver);
    }

    @Bean
    public PromptSpecValidator promptSpecValidator(ObjectiveRegistry objectiveRegistry) {
        return new PromptSpecValidator(objectiveRegistry);
    }

    @Bean
    public RecommendationRegistry recommendationRegistry() {
        return new RecommendationRegistry();
    }

    @Bean
    public BadgeResolver badgeResolver() {
        return new BadgeResolver();
    }
}
