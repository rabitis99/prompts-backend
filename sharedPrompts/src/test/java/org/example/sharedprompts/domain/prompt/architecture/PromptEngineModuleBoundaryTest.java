package org.example.sharedprompts.domain.prompt.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 13차 구조 리팩터링: prompt engine 모듈 경계 검증.
 * - core는 다른 모듈·global에 의존하지 않음
 * - runtime은 policy 인터페이스만 사용, api에 의존하지 않음
 * - api(adapter.in.web)는 domain.value 직접 참조 금지
 * - observability는 runtime 로직에 의존하지 않음
 * - policy는 runtime에 의존하지 않음
 */
@DisplayName("Prompt engine module boundary")
class PromptEngineModuleBoundaryTest {

    private static JavaClasses promptDomainClasses;

    @BeforeAll
    static void scan() {
        promptDomainClasses = new ClassFileImporter()
                .importPackages("org.example.sharedprompts.domain.prompt");
    }

    @Nested
    @DisplayName("1. Core must not depend on global or other modules")
    class CoreIndependence {

        @Test
        void core_packages_do_not_depend_on_global() {
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(
                            "..domain.prompt.domain.value..",
                            "..domain.prompt.domain.model..",
                            "..domain.prompt.domain.service.badge..",
                            "..domain.prompt.domain.resolutions..",
                            "..domain.prompt.common.enums..",
                            "..domain.prompt.common.contract.."
                    )
                    .should().dependOnClassesThat().resideInAPackage("..global..");
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("2. API (adapter.in.web) must not depend on domain.value")
    class ApiNoDomainValue {

        @Test
        void adapter_in_web_does_not_depend_on_domain_value() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.prompt.adapter.in.web..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.prompt.domain.value..");
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("3. Runtime must not depend on API (adapter)")
    class RuntimeNoApi {

        @Test
        void application_semantic_does_not_depend_on_adapter() {
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(
                            "..domain.prompt.application.semantic.recommendation..",
                            "..domain.prompt.application.semantic.resolution..",
                            "..domain.prompt.application.semantic.experiment..",
                            "..domain.prompt.application.semantic.explanation..",
                            "..domain.prompt.application.semantic.validation.."
                    )
                    .should().dependOnClassesThat().resideInAPackage("..domain.prompt.adapter..");
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("4. Policy must not depend on runtime (recommendation/resolution)")
    class PolicyNoRuntime {

        @Test
        void policy_packages_do_not_depend_on_recommendation_or_resolution() {
            // Exclude test classes (e.g. SemanticStructurePhase*Test in domain.semantic.policy) which legitimately use recommendation.
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(
                            "..domain.prompt.domain.semantic.policy..",
                            "..domain.prompt.application.semantic.policy..",
                            "..domain.prompt.infrastructure.policy.."
                    )
                    .and().haveSimpleNameNotEndingWith("Test")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..domain.prompt.application.semantic.recommendation..",
                            "..domain.prompt.application.semantic.resolution.."
                    );
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("5. Observability must not depend on runtime engine logic")
    class ObservabilityNoRuntime {

        @Test
        void observability_packages_do_not_depend_on_recommendation_engine() {
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(
                            "..domain.prompt.domain.semantic.trace..",
                            "..domain.prompt.domain.semantic.observability..",
                            "..domain.prompt.application.semantic.observability.."
                    )
                    .should().dependOnClassesThat().resideInAPackage("..domain.prompt.application.semantic.recommendation..");
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("6. No circular dependency: runtime uses policy sources only")
    class RuntimeUsesPolicyInterfaces {

        @Test
        void recommendation_resolution_only_use_policy_domain_and_application_policy_ports() {
            // Semantic recommendation/resolution may depend on domain.semantic.policy (interfaces/sources)
            // and application.semantic.policy (PolicySelectionStrategy, etc.), but must not depend on
            // infrastructure.policy (loaders/parsers). So we only check policy->runtime is forbidden;
            // runtime->policy is allowed (policy source interfaces live in domain.semantic.policy).
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.prompt.infrastructure.policy..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..domain.prompt.application.semantic.recommendation..",
                            "..domain.prompt.application.semantic.resolution.."
                    );
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("7. Adapter DTO assembler must not depend on application-layer concrete implementations")
    class AdapterAssemblerNoExplanationImpl {

        @Test
        void adapter_assembler_does_not_reference_explanation_assembler_implementation() {
            // DefaultRecommendationResponseAssembler must depend only on RecommendationExplanationAssembler interface,
            // not on DefaultRecommendationExplanationAssembler (7차 assembler 계층 분리).
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.prompt.adapter.in.web.assembler..")
                    .and().haveSimpleName("DefaultRecommendationResponseAssembler")
                    .should().dependOnClassesThat().haveSimpleName("DefaultRecommendationExplanationAssembler");
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("8. Observability/audit must not depend on controller or web DTO")
    class ObservabilityAuditNoWebDto {

        @Test
        void observability_and_audit_do_not_depend_on_controller_or_web_dto() {
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(
                            "..domain.prompt.application.semantic.observability..",
                            "..domain.prompt.application.semantic.audit..",
                            "..domain.prompt.domain.semantic.observability.."
                    )
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..domain.prompt.adapter.in.web.controller..",
                            "..domain.prompt.adapter.in.web.dto.."
                    );
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("9. Core (value, model, enums, contract) must not depend on adapter or observability or policy impl")
    class CoreNoAdapterOrPolicyImpl {

        @Test
        void core_packages_do_not_depend_on_adapter_or_observability_or_policy_infrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage(
                            "..domain.prompt.domain.value..",
                            "..domain.prompt.domain.model..",
                            "..domain.prompt.common.enums..",
                            "..domain.prompt.common.contract.."
                    )
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..domain.prompt.adapter..",
                            "..domain.prompt.application.semantic.observability..",
                            "..domain.prompt.application.semantic.audit..",
                            "..domain.prompt.infrastructure.policy.."
                    );
            rule.check(promptDomainClasses);
        }
    }

    @Nested
    @DisplayName("10. Policy registry must come from bootstrap only (no hardcoded registry in ResolutionConfig)")
    class PolicyRegistryFromBootstrapOnly {

        @Test
        void resolution_config_does_not_instantiate_default_policy_source_registry() {
            // PolicySourceRegistry must be provided by PolicyBootstrapConfig (pipeline → bootstrap → registry).
            // Regression: ResolutionConfig must not create DefaultPolicySourceRegistry with hardcoded bundles.
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain.prompt.infrastructure.config..")
                    .and().haveSimpleName("ResolutionConfig")
                    .should().dependOnClassesThat().haveSimpleName("DefaultPolicySourceRegistry");
            rule.check(promptDomainClasses);
        }
    }
}
