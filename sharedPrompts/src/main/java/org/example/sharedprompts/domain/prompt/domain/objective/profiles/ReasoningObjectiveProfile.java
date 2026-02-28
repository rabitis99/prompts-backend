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

public final class ReasoningObjectiveProfile extends BaseObjectiveProfile {

    /** REASONING: 불확실성 체크 O, 입력 보존 체크 X */
    private static final VerificationStrategy VERIFIER = new StandardVerification(true, false);

    private static final PromptStrategyBundle DEFAULT_BUNDLE =
            PromptStrategyBundle.of("REASONING_DEFAULT", EnumSet.of(
                    PromptingStrategy.CLARIFY_FIRST,
                    PromptingStrategy.STEP_BY_STEP,
                    PromptingStrategy.CHECKLIST_VERIFY,
                    PromptingStrategy.REQUIRE_JUSTIFICATION
            ));

    private static final QualityRubric RUBRIC = QualityRubric.of(List.of(
            QualityRubric.RubricItem.COVERAGE,
            QualityRubric.RubricItem.NO_PROHIBITED_CONTENT,
            QualityRubric.RubricItem.NO_CONTRADICTION,
            QualityRubric.RubricItem.UNCERTAINTY_HANDLING
    ));

    @Override public PromptObjective objective()              { return PromptObjective.REASONING; }
    @Override public int maxLlmCallCount()                    { return 2; }
    @Override public boolean supportsConstrainedDecoding()    { return false; }
    @Override public QualityPriority priority()               { return QualityPriority.ACCURACY_FIRST; }
    @Override public QualityRubric rubric()                   { return RUBRIC; }
    @Override public VerificationStrategy verificationStrategy() { return VERIFIER; }
    @Override public PromptStrategyBundle defaultBundle()     { return DEFAULT_BUNDLE; }
    @Override public List<PromptSection> extraSections()      { return List.of(); }

    @Override
    public Constraints constraints(ExperienceLevel level) {
        boolean requireCitations = level == ExperienceLevel.EXPERT;
        return buildConstraints(2500, true, requireCitations, level);
    }

    @Override
    public OutputContract outputContract(String jsonSchema, int maxTokens) {
        return freeTextContract(maxTokens);
    }

    @Override
    public String instructionContent(LanguageType locale) {
        return i18n(locale,
                "문제를 단계적으로 분석하고 각 단계의 추론을 명확히 제시하세요.",
                "Analyze the problem step-by-step. Show your reasoning process clearly at each stage.",
                "問題を段階的に分析し、各段階の推論を明確に示してください。");
    }
}
