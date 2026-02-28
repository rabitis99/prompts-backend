package org.example.sharedprompts.domain.prompt.domain.objective.profiles;

import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.domain.value.QualityPriority;
import org.example.sharedprompts.domain.prompt.domain.verification.ChainOfVerification;
import org.example.sharedprompts.domain.prompt.domain.verification.VerificationStrategy;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;

import java.util.EnumSet;
import java.util.List;

public final class FactualObjectiveProfile extends BaseObjectiveProfile {

    private static final VerificationStrategy VERIFIER = new ChainOfVerification();

    private static final PromptStrategyBundle DEFAULT_BUNDLE =
            PromptStrategyBundle.of("FACTUAL_DEFAULT", EnumSet.of(
                    PromptingStrategy.CLARIFY_FIRST,
                    PromptingStrategy.STEP_BY_STEP,
                    PromptingStrategy.CHECKLIST_VERIFY,
                    PromptingStrategy.CHAIN_OF_VERIFICATION,
                    PromptingStrategy.CITE_OR_UNCERTAIN
            ));

    private static final QualityRubric RUBRIC = QualityRubric.of(List.of(
            QualityRubric.RubricItem.COVERAGE,
            QualityRubric.RubricItem.NO_PROHIBITED_CONTENT,
            QualityRubric.RubricItem.INPUT_PRESERVATION,
            QualityRubric.RubricItem.NO_CONTRADICTION,
            QualityRubric.RubricItem.UNCERTAINTY_HANDLING
    ));

    @Override public PromptObjective objective()              { return PromptObjective.FACTUAL; }
    @Override public int maxLlmCallCount()                    { return 4; }
    @Override public boolean supportsConstrainedDecoding()    { return false; }
    @Override public QualityPriority priority()               { return QualityPriority.ACCURACY_FIRST; }
    @Override public QualityRubric rubric()                   { return RUBRIC; }
    @Override public VerificationStrategy verificationStrategy() { return VERIFIER; }
    @Override public PromptStrategyBundle defaultBundle()     { return DEFAULT_BUNDLE; }
    @Override public List<PromptSection> extraSections()      { return List.of(); }

    @Override
    public Constraints constraints(ExperienceLevel level) {
        boolean requireCitations = true;
        return buildConstraints(3000, false, requireCitations, level);
    }

    @Override
    public OutputContract outputContract(String jsonSchema, int maxTokens) {
        return freeTextContract(maxTokens);
    }

    @Override
    public String instructionContent(LanguageType locale) {
        return i18n(locale,
                "정확하고 근거 기반으로 답변하세요. 필요 시 출처 또는 불확실성 표기를 포함하세요.",
                "Provide accurate, well-sourced information. Include citations or uncertainty markers where appropriate.",
                "正確かつ根拠に基づいて回答してください。必要に応じて出典または不確実性の表記を含めてください。");
    }
}
