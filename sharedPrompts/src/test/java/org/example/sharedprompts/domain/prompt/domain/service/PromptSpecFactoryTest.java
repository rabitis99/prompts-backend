package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.objective.DefaultObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.AnalyticalObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.CreativeObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ExtractionObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.FactualObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.PlanningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.profiles.ReasoningObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolution.ExplicitObjectiveMapping;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolution.ObjectiveResolverPort;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.role.EtcRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PromptSpecFactory 단위 테스트")
class PromptSpecFactoryTest {

    private PromptSpecFactory factory;
    private ObjectiveRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultObjectiveRegistry(java.util.List.of(
                new FactualObjectiveProfile(),
                new ReasoningObjectiveProfile(),
                new ExtractionObjectiveProfile(),
                new PlanningObjectiveProfile(),
                new CreativeObjectiveProfile(),
                new AnalyticalObjectiveProfile()
        ));
        factory = new PromptSpecFactory(registry, new StrategyBundlePolicy(registry),
                new ObjectiveResolver(new ExplicitObjectiveMapping(), new ObjectiveMappingRegistry()));
    }

    @Test
    @DisplayName("ANALYTICAL TaskDomain → FACTUAL Objective 매핑")
    void analyticalDomainMapsToFactualObjective() {
        PromptSpec spec = factory.create(
                "테스트 입력",
                TaskDomain.ANALYTICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.FACTUAL);
        assertThat(spec.getTaskDomain()).isEqualTo(TaskDomain.ANALYTICAL);
    }

    @Test
    @DisplayName("CREATIVE TaskDomain → CREATIVE_WITH_CONSTRAINTS Objective 매핑")
    void creativeDomainMapsToCreativeObjective() {
        PromptSpec spec = factory.create(
                "창의적인 입력",
                TaskDomain.CREATIVE,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.CREATIVE_WITH_CONSTRAINTS);
    }

    @Test
    @DisplayName("PRACTICAL TaskDomain → PLANNING Objective 매핑")
    void practicalDomainMapsToPlanning() {
        PromptSpec spec = factory.create(
                "실용적 입력",
                TaskDomain.PRACTICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.PLANNING);
    }

    @Test
    @DisplayName("ActionType override Objective가 있으면 최우선으로 사용된다")
    void actionTypeDefaultObjectiveHasHighestPriority() {
        ActionTypeWithDefault actionType = new ActionTypeWithDefault(PromptObjective.EXTRACTION, "custom_action");

        PromptSpec spec = factory.create(
                "입력",
                TaskDomain.TECHNICAL,
                actionType,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.INTERMEDIATE
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.EXTRACTION);
    }

    @Test
    @DisplayName("ActionType override가 없으면 이름 휴리스틱으로 Objective를 추론한다")
    void heuristicMappingUsedWhenNoOverride() {
        ActionTypeWithDefault summarizeAction = new ActionTypeWithDefault(null, "SUMMARIZE_NOTES");

        PromptSpec spec = factory.create(
                "입력",
                TaskDomain.GENERAL,
                summarizeAction,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.INTERMEDIATE
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.FACTUAL);
    }

    @Test
    @DisplayName("ActionType 매핑이 없으면 TaskDomain 기본 Objective를 사용한다")
    void domainDefaultUsedWhenNoMapping() {
        ActionTypeWithDefault unknownAction = new ActionTypeWithDefault(null, "UNKNOWN_ACTION");

        PromptSpec spec = factory.create(
                "입력",
                TaskDomain.PRACTICAL,
                unknownAction,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.INTERMEDIATE
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.PLANNING);
    }

    @Test
    @DisplayName("FACTUAL Objective → QualityRubric에 UNCERTAINTY_HANDLING 포함")
    void factualObjectiveHasUncertaintyHandlingRubric() {
        PromptSpec spec = factory.create(
                "사실 기반 입력",
                TaskDomain.ANALYTICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        assertThat(spec.getRubric().getItems())
                .contains(QualityRubric.RubricItem.UNCERTAINTY_HANDLING);
    }

    @Test
    @DisplayName("TECHNICAL TaskDomain → REASONING Objective 및 StrategyBundle 생성")
    void technicalDomainHasReasoningObjectiveAndStrategyBundle() {
        PromptSpec spec = factory.create(
                "기술적 입력",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        assertThat(spec.getObjective()).isEqualTo(PromptObjective.REASONING);
        assertThat(spec.getStrategyBundle()).isNotNull();
    }

    @Test
    @DisplayName("Experimental 비활성화 시 Experimental 전략 제외")
    void experimentalDisabledExcludesExperimentalStrategies() {
        PromptSpec spec = factory.create(
                "입력",
                TaskDomain.ANALYTICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false  // experimental disabled
        );

        boolean hasExperimental = spec.getStrategyBundle().getStrategies().stream()
                .anyMatch(PromptingStrategy::isExperimental);
        assertThat(hasExperimental).isFalse();
    }

    @Test
    @DisplayName("Experimental 활성화 시 Experimental 전략 포함")
    void experimentalEnabledIncludesExperimentalStrategies() {
        PromptSpec spec = factory.create(
                "입력",
                TaskDomain.ANALYTICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                true  // experimental enabled
        );

        boolean hasExperimental = spec.getStrategyBundle().getStrategies().stream()
                .anyMatch(PromptingStrategy::isExperimental);
        assertThat(hasExperimental).isTrue();
    }

    @Test
    @DisplayName("PromptSpec은 Core 전략을 항상 포함한다")
    void specAlwaysIncludesCoreStrategies() {
        PromptSpec spec = factory.create(
                "테스트",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        assertThat(spec.getStrategyBundle().getStrategies())
                .contains(PromptingStrategy.CLARIFY_FIRST);
    }

    @Test
    @DisplayName("rawInput이 null이면 예외 발생")
    void nullInputThrowsException() {
        assertThatThrownBy(() ->
            factory.create(null, TaskDomain.GENERAL, null, null, null, null, null, false)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("FACTUAL Objective Rubric에는 COVERAGE와 NO_PROHIBITED_CONTENT가 항상 포함된다")
    void factualRubricAlwaysHasCommonItems() {
        QualityRubric rubric = registry.get(PromptObjective.FACTUAL).rubric();

        assertThat(rubric.getItems())
                .contains(QualityRubric.RubricItem.COVERAGE,
                          QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
    }

    @Test
    @DisplayName("CREATIVE_WITH_CONSTRAINTS Rubric에는 FORMAT_COMPLIANCE 포함, UNCERTAINTY_HANDLING 미포함")
    void creativeRubricHasFormatButNotUncertainty() {
        QualityRubric rubric = registry.get(PromptObjective.CREATIVE_WITH_CONSTRAINTS).rubric();

        assertThat(rubric.getItems()).contains(QualityRubric.RubricItem.FORMAT_COMPLIANCE);
        assertThat(rubric.getItems()).doesNotContain(QualityRubric.RubricItem.UNCERTAINTY_HANDLING);
    }

    @Test
    @DisplayName("BEGINNER 경험 레벨은 INTERMEDIATE보다 더 긴 maxLength와 step-by-step을 요구한다")
    void beginnerHasLongerMaxLengthAndStepByStep() {
        PromptSpec intermediate = factory.create(
                "입력",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.INTERMEDIATE
        );

        PromptSpec beginner = factory.create(
                "입력",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.BEGINNER
        );

        assertThat(beginner.getConstraints().getMaxLength())
                .isGreaterThan(intermediate.getConstraints().getMaxLength());
        assertThat(beginner.getConstraints().isRequireStepByStep()).isTrue();
    }

    @Test
    @DisplayName("EXPERT 경험 레벨은 INTERMEDIATE보다 짧은 maxLength를 가진다")
    void expertHasShorterMaxLength() {
        PromptSpec intermediate = factory.create(
                "입력",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.INTERMEDIATE
        );

        PromptSpec expert = factory.create(
                "입력",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false,
                ExperienceLevel.EXPERT
        );

        assertThat(expert.getConstraints().getMaxLength())
                .isLessThan(intermediate.getConstraints().getMaxLength());
    }

    private static final class ActionTypeWithDefault implements org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface {
        private final PromptObjective defaultObjective;
        private final String name;

        private ActionTypeWithDefault(PromptObjective defaultObjective, String name) {
            this.defaultObjective = defaultObjective;
            this.name = name;
        }

        @Override
        public String getDisplayNameKo() {
            return name;
        }

        @Override
        public String getDisplayNameEn() {
            return name;
        }

        @Override
        public String getDisplayNameJa() {
            return name;
        }

        @Override
        public PromptObjective getDefaultObjective() {
            return defaultObjective;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
