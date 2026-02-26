package org.example.sharedprompts.domain.prompt.domain.service;

import org.example.sharedprompts.domain.prompt.domain.model.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.ContentSandbox;
import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.domain.value.QualityPriority;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptSpecValidator 단위 테스트")
class PromptSpecValidatorTest {

    private PromptSpecValidator validator;
    private PromptSpecFactory factory;

    @BeforeEach
    void setUp() {
        validator = new PromptSpecValidator();
        factory = new PromptSpecFactory(new StrategyBundlePolicy());
    }

    private PromptSpec buildSpec(PromptObjective objective, String input) {
        return PromptSpec.builder()
                .objective(objective)
                .priority(QualityPriority.ACCURACY_FIRST)
                .rubric(factory.buildRubric(objective))
                .strategyBundle(PromptStrategyBundle.coreOnly())
                .rawInput(input)
                .clarifiedInput(input)
                .contentSandbox(ContentSandbox.defaults())
                .outputContract(OutputContract.freeText(2000))
                .constraints(Constraints.defaults())
                .tone(ToneType.NEUTRAL)
                .style(StyleType.NARRATIVE)
                .locale(LanguageType.KOREAN)
                .build();
    }

    @Nested
    @DisplayName("FACTUAL Verify (CHAIN_OF_VERIFICATION 모드)")
    class FactualVerify {

        @Test
        @DisplayName("정상 초안은 pass")
        void normalDraftPasses() {
            PromptSpec spec = buildSpec(PromptObjective.FACTUAL, "AI 기술 동향 분석");
            String goodDraft = "AI 기술 동향에 대해 AI 분야에서의 최신 트렌드를 분석합니다. " +
                    "각 주장은 참고 문헌을 기반으로 작성하며 불확실한 수치는 '추정'으로 표기합니다.";

            VerifyResult result = validator.verify(goodDraft, spec);

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("금지어 포함 시 fail")
        void prohibitedContentFails() {
            PromptSpec spec = buildSpec(PromptObjective.FACTUAL, "테스트");
            String draft = "이 분석에는 주민등록번호를 포함한 데이터가 사용되었습니다.";

            VerifyResult result = validator.verify(draft, spec);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getFailedItems()).contains(QualityRubric.RubricItem.NO_PROHIBITED_CONTENT);
        }

        @Test
        @DisplayName("너무 짧은 draft는 COVERAGE fail")
        void tooShortDraftFailsCoverage() {
            PromptSpec spec = buildSpec(PromptObjective.FACTUAL, "테스트");
            String shortDraft = "짧음";

            VerifyResult result = validator.verify(shortDraft, spec);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getFailedItems()).contains(QualityRubric.RubricItem.COVERAGE);
        }
    }

    @Nested
    @DisplayName("EXTRACTION Verify (SCHEMA_FIRST 모드)")
    class ExtractionVerify {

        @Test
        @DisplayName("JSON 형식 초안은 FORMAT_COMPLIANCE pass")
        void jsonDraftPassesFormatCompliance() {
            PromptSpec spec = PromptSpec.builder()
                    .objective(PromptObjective.EXTRACTION)
                    .priority(QualityPriority.STRUCTURE_FIRST)
                    .rubric(factory.buildRubric(PromptObjective.EXTRACTION))
                    .strategyBundle(PromptStrategyBundle.coreOnly())
                    .rawInput("데이터 추출")
                    .clarifiedInput("데이터를 추출하세요. 필드: name, age")
                    .contentSandbox(ContentSandbox.defaults())
                    .outputContract(OutputContract.jsonStructured(
                            """
                            {"type":"object","properties":{"result":{"type":"string"}}}
                            """, 500))
                    .constraints(Constraints.defaults())
                    .tone(ToneType.NEUTRAL)
                    .style(StyleType.NARRATIVE)
                    .locale(LanguageType.KOREAN)
                    .build();

            String jsonDraft = "{\"result\": \"추출된 데이터입니다\"}";

            VerifyResult result = validator.verify(jsonDraft, spec);

            assertThat(result.getItemResults().get(QualityRubric.RubricItem.FORMAT_COMPLIANCE)).isTrue();
        }

        @Test
        @DisplayName("JSON 아닌 응답은 FORMAT_COMPLIANCE fail")
        void nonJsonDraftFailsFormatCompliance() {
            PromptSpec spec = PromptSpec.builder()
                    .objective(PromptObjective.EXTRACTION)
                    .priority(QualityPriority.STRUCTURE_FIRST)
                    .rubric(factory.buildRubric(PromptObjective.EXTRACTION))
                    .strategyBundle(PromptStrategyBundle.coreOnly())
                    .rawInput("데이터")
                    .contentSandbox(ContentSandbox.defaults())
                    .outputContract(OutputContract.jsonStructured("{\"type\":\"object\"}", 500))
                    .constraints(Constraints.defaults())
                    .tone(ToneType.NEUTRAL)
                    .style(StyleType.NARRATIVE)
                    .locale(LanguageType.KOREAN)
                    .build();

            String plainText = "이것은 JSON이 아닌 일반 텍스트입니다.";

            VerifyResult result = validator.verify(plainText, spec);

            assertThat(result.getItemResults().get(QualityRubric.RubricItem.FORMAT_COMPLIANCE)).isFalse();
        }
    }

    @Nested
    @DisplayName("CREATIVE_WITH_CONSTRAINTS Verify (SOFT 모드)")
    class CreativeVerify {

        @Test
        @DisplayName("Soft-verify: 금지어·명백한 모순 없으면 pass")
        void normalCreativeDraftPasses() {
            PromptSpec spec = buildSpec(PromptObjective.CREATIVE_WITH_CONSTRAINTS, "창의적인 이야기");
            String creativeDraft = "이 이야기는 상상력을 자극하는 독창적인 내용으로 구성되어 있습니다. " +
                    "다양한 관점에서 아이디어를 탐구하며 독자의 감성을 자극합니다.";

            VerifyResult result = validator.verify(creativeDraft, spec);

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("Soft-verify: COVERAGE 항목은 체크하지 않는다 (Soft 모드)")
        void softVerifyDoesNotCheckCoverage() {
            PromptSpec spec = buildSpec(PromptObjective.CREATIVE_WITH_CONSTRAINTS, "창의적 내용");
            String shortCreative = "짧은 창의적 내용"; // 50자 미만이지만 SOFT 모드에서는 COVERAGE 불검사

            VerifyResult result = validator.verify(shortCreative, spec);

            // SOFT 모드에서 COVERAGE는 검증하지 않으므로 해당 항목 결과가 없거나 관계없음
            assertThat(result.getItemResults().containsKey(QualityRubric.RubricItem.COVERAGE)).isFalse();
        }
    }

    @Test
    @DisplayName("Repair를 위한 실패 항목 추출 — getFailedItems()")
    void failedItemsAreExtractedCorrectly() {
        PromptSpec spec = buildSpec(PromptObjective.FACTUAL, "테스트");
        String draft = "주민등록번호 포함 정보"; // 금지어 + 너무 짧음

        VerifyResult result = validator.verify(draft, spec);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getFailedItems()).isNotEmpty();
        assertThat(result.getFailureReasons()).isNotEmpty();
    }
}
