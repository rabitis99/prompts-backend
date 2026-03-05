package org.example.sharedprompts.domain.prompt.domain.verification.guideline;

import org.example.sharedprompts.domain.prompt.common.guideline.i18n.I18nText;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuidelineVerifier 단위 테스트")
class GuidelineVerifierTest {

    private final GuidelineVerifier verifier = new GuidelineVerifier(new DefaultGuidelineRuleChecker());

    @Test
    @DisplayName("빈 규칙 목록이면 통과")
    void emptyRulesPasses() {
        GuidelineVerificationResult result = verifier.verify("Some output", List.of());
        assertThat(result.passed()).isTrue();
        assertThat(result.failures()).isEmpty();
    }

    @Test
    @DisplayName("FORBID 규칙 위반 시 실패")
    void forbidViolationFails() {
        GuidelineRule noGreeting = new GuidelineRule(
                "TEST.NO_GREETING",
                I18nText.of("인사 금지", "No greeting", "挨拶禁止"),
                I18nText.of("인사말 금지", "Do not include greetings", "挨拶禁止"),
                RuleLevel.HARD,
                RuleType.FORBID
        );
        GuidelineVerificationResult result = verifier.verify("Hello! Here is the content.", List.of(noGreeting));
        assertThat(result.passed()).isFalse();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).ruleId()).isEqualTo("TEST.NO_GREETING");
    }

    @Test
    @DisplayName("FORBID 규칙 준수 시 통과")
    void forbidCompliancePasses() {
        GuidelineRule noGreeting = new GuidelineRule(
                "TEST.NO_GREETING",
                I18nText.of("인사 금지", "No greeting", "挨拶禁止"),
                I18nText.of("인사말 금지", "Do not include greetings", "挨拶禁止"),
                RuleLevel.HARD,
                RuleType.FORBID
        );
        GuidelineVerificationResult result = verifier.verify("Here is the content without greeting.", List.of(noGreeting));
        assertThat(result.passed()).isTrue();
        assertThat(result.failures()).isEmpty();
    }

    @Test
    @DisplayName("REQUIRE 규칙은 기본 체커에서 위반으로 처리하지 않음")
    void requireRulesNoDefaultViolation() {
        GuidelineRule require = new GuidelineRule(
                "TEST.REQUIRE",
                I18nText.of("필수", "Required", "必須"),
                I18nText.of("필수 내용 포함", "Include required content", "必須内容"),
                RuleLevel.HARD,
                RuleType.REQUIRE
        );
        GuidelineVerificationResult result = verifier.verify("Any content", List.of(require));
        assertThat(result.passed()).isTrue();
    }
}
