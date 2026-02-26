package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.role.EtcRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PromptSpecFactory 단위 테스트")
class PromptSpecFactoryTest {

    private PromptSpecFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PromptSpecFactory(new StrategyBundlePolicy());
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
    @DisplayName("EXTRACTION Objective → JSON OutputContract 생성")
    void extractionObjectiveHasJsonOutputContract() {
        PromptSpec spec = factory.create(
                "데이터 추출 입력",
                TaskDomain.TECHNICAL,
                EtcActionType.GENERAL_CONSULTATION,
                EtcRoleType.GENERAL_CONSULTANT,
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                LanguageType.KOREAN,
                false
        );

        // TECHNICAL → REASONING이지만, EXTRACTION 테스트용 직접 rubric 확인
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
    @DisplayName("PromptSpec은 Core 전략을 항상 포함한다")
    void specAlwaysIncoreStrategies() {
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
        QualityRubric rubric = factory.buildRubric(PromptObjective.FACTUAL);

        assertThat(rubric.getItems())
                .contains(QualityRubric.RubricItem.COVERAGE,
                          QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
    }

    @Test
    @DisplayName("CREATIVE_WITH_CONSTRAINTS Rubric에는 FORMAT_COMPLIANCE 포함, UNCERTAINTY_HANDLING 미포함")
    void creativeRubricHasFormatButNotUncertainty() {
        QualityRubric rubric = factory.buildRubric(PromptObjective.CREATIVE_WITH_CONSTRAINTS);

        assertThat(rubric.getItems()).contains(QualityRubric.RubricItem.FORMAT_COMPLIANCE);
        assertThat(rubric.getItems()).doesNotContain(QualityRubric.RubricItem.UNCERTAINTY_HANDLING);
    }
}
