package org.example.sharedprompts.domain.prompt.domain.objective.profiles;

import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.domain.value.QualityPriority;
import org.example.sharedprompts.domain.prompt.domain.verification.SchemaFirstVerification;
import org.example.sharedprompts.domain.prompt.domain.verification.VerificationStrategy;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;

import java.util.EnumSet;
import java.util.List;

public final class ExtractionObjectiveProfile extends BaseObjectiveProfile {

    private static final VerificationStrategy VERIFIER = new SchemaFirstVerification();

    private static final PromptStrategyBundle DEFAULT_BUNDLE =
            PromptStrategyBundle.of("EXTRACTION_DEFAULT", EnumSet.of(
                    PromptingStrategy.CLARIFY_FIRST,
                    PromptingStrategy.STEP_BY_STEP,
                    PromptingStrategy.CHECKLIST_VERIFY
            ));

    private static final QualityRubric RUBRIC = QualityRubric.of(List.of(
            QualityRubric.RubricItem.COVERAGE,
            QualityRubric.RubricItem.NO_PROHIBITED_CONTENT,
            QualityRubric.RubricItem.FORMAT_COMPLIANCE,
            QualityRubric.RubricItem.INPUT_PRESERVATION
    ));

    private static final List<PromptSection> EXTRA_SECTIONS = List.of(
            PromptSection.required(
                    PromptSection.SectionType.OUTPUT_FORMAT,
                    "Respond strictly in JSON that matches the provided schema. "
                            + "Do not include explanations, comments, or additional fields."
            )
    );

    @Override public PromptObjective objective()              { return PromptObjective.EXTRACTION; }
    @Override public int maxLlmCallCount()                    { return 1; }
    @Override public boolean supportsConstrainedDecoding()    { return true; }
    @Override public QualityPriority priority()               { return QualityPriority.STRUCTURE_FIRST; }
    @Override public QualityRubric rubric()                   { return RUBRIC; }
    @Override public VerificationStrategy verificationStrategy() { return VERIFIER; }
    @Override public PromptStrategyBundle defaultBundle()     { return DEFAULT_BUNDLE; }
    @Override public List<PromptSection> extraSections()      { return EXTRA_SECTIONS; }

    @Override
    public Constraints constraints(ExperienceLevel level) {
        return buildConstraints(1000, false, false, level);
    }

    @Override
    public OutputContract outputContract(String jsonSchema, int maxTokens) {
        String schema = (jsonSchema != null && !jsonSchema.isBlank()) ? jsonSchema : DEFAULT_EXTRACTION_SCHEMA;
        int extractionMax = Math.min(maxTokens, 1000);
        return OutputContract.jsonStructured(schema, extractionMax);
    }

    @Override
    public String instructionContent(LanguageType locale) {
        return i18n(locale,
                "요청된 정보를 지정된 출력 스키마에 맞게 엄격히 추출·구조화하세요.",
                "Extract and structure the requested information strictly according to the specified output schema.");
    }
}
