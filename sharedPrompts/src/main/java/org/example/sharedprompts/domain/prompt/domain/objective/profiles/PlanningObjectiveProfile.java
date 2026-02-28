package org.example.sharedprompts.domain.prompt.domain.objective.profiles;

import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.domain.value.QualityPriority;
import org.example.sharedprompts.domain.prompt.domain.verification.StandardVerification;
import org.example.sharedprompts.domain.prompt.domain.verification.VerificationStrategy;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;

import java.util.EnumSet;
import java.util.List;

public final class PlanningObjectiveProfile extends BaseObjectiveProfile {

    /** PLANNING: 불확실성 체크 X, 입력 보존 체크 O */
    private static final VerificationStrategy VERIFIER = new StandardVerification(false, true);

    private static final PromptStrategyBundle DEFAULT_BUNDLE =
            PromptStrategyBundle.of("PLANNING_DEFAULT", EnumSet.of(
                    PromptingStrategy.CLARIFY_FIRST,
                    PromptingStrategy.STEP_BY_STEP,
                    PromptingStrategy.CHECKLIST_VERIFY,
                    PromptingStrategy.DECOMPOSITION,
                    PromptingStrategy.EDGE_CASE_SCAN
            ));

    private static final QualityRubric RUBRIC = QualityRubric.of(List.of(
            QualityRubric.RubricItem.COVERAGE,
            QualityRubric.RubricItem.NO_PROHIBITED_CONTENT,
            QualityRubric.RubricItem.INPUT_PRESERVATION,
            QualityRubric.RubricItem.NO_CONTRADICTION
    ));

    private static final List<PromptSection> EXTRA_SECTIONS = List.of(
            PromptSection.optional(PromptSection.SectionType.CONSTRAINTS, "")
    );

    @Override public PromptObjective objective()              { return PromptObjective.PLANNING; }
    @Override public int maxLlmCallCount()                    { return 3; }
    @Override public boolean supportsConstrainedDecoding()    { return false; }
    @Override public QualityPriority priority()               { return QualityPriority.STRUCTURE_FIRST; }
    @Override public QualityRubric rubric()                   { return RUBRIC; }
    @Override public VerificationStrategy verificationStrategy() { return VERIFIER; }
    @Override public PromptStrategyBundle defaultBundle()     { return DEFAULT_BUNDLE; }
    @Override public List<PromptSection> extraSections()      { return EXTRA_SECTIONS; }

    @Override
    public Constraints constraints(ExperienceLevel level) {
        return buildConstraints(3000, true, false, level);
    }

    @Override
    public OutputContract outputContract(String jsonSchema, int maxTokens) {
        return freeTextContract(maxTokens);
    }

    @Override
    public String instructionContent(LanguageType locale) {
        return i18n(locale,
                "실행 가능한 계획을 단계, 의존성, 성공 기준과 함께 구조화해 제시하세요.",
                "Create a structured, actionable plan with clear steps, dependencies, and success criteria.");
    }
}
