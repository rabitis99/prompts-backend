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
import org.example.sharedprompts.domain.prompt.domain.descriptor.RoleDescriptorPort;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.guideline.bundle.GuidelineBundleBuilder;
import org.example.sharedprompts.domain.prompt.domain.verification.guideline.DefaultGuidelineRuleChecker;
import org.example.sharedprompts.domain.prompt.domain.verification.guideline.GuidelineVerifier;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDefinitionProviderAssembly;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
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
@Import({ActionTypeRegistryConfig.class, ResolutionConfig.class, ObservabilityConfig.class, AuditConfig.class})
public class PromptDomainConfig {

    @Bean
    public IntentDictionary intentDictionary() {
        return IntentDefinitionProviderAssembly.productionIntentDictionary();
    }

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
    public GuidelineBundleBuilder guidelineBundleBuilder() {
        return new GuidelineBundleBuilder();
    }

    @Bean
    public PromptSpecFactory promptSpecFactory(ObjectiveRegistry objectiveRegistry,
                                               StrategyBundlePolicy strategyBundlePolicy,
                                               ObjectiveResolverPort objectiveResolver,
                                               GuidelineBundleBuilder guidelineBundleBuilder,
                                               RoleDescriptorPort roleDescriptorPort,
                                               CanonicalActionRegistry canonicalActionRegistry) {
        return new PromptSpecFactory(objectiveRegistry, strategyBundlePolicy, objectiveResolver, guidelineBundleBuilder, roleDescriptorPort, canonicalActionRegistry);
    }

    @Bean
    public GuidelineVerifier guidelineVerifier() {
        return new GuidelineVerifier(new DefaultGuidelineRuleChecker());
    }

    @Bean
    public PromptSpecValidator promptSpecValidator(ObjectiveRegistry objectiveRegistry,
                                                   GuidelineVerifier guidelineVerifier) {
        return new PromptSpecValidator(objectiveRegistry, guidelineVerifier);
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
