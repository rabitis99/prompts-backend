package org.example.sharedprompts.domain.prompt.domain.service.spec;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleLevel;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.verification.VerificationContext;
import org.example.sharedprompts.domain.prompt.domain.verification.guideline.GuidelineVerificationResult;
import org.example.sharedprompts.domain.prompt.domain.verification.guideline.GuidelineVerifier;

import java.util.ArrayList;
import java.util.List;

public class PromptSpecValidator {

    private final ObjectiveRegistry objectiveRegistry;
    private final GuidelineVerifier guidelineVerifier;

    public PromptSpecValidator(ObjectiveRegistry objectiveRegistry) {
        this(objectiveRegistry, null);
    }

    public PromptSpecValidator(ObjectiveRegistry objectiveRegistry, GuidelineVerifier guidelineVerifier) {
        this.objectiveRegistry = objectiveRegistry;
        this.guidelineVerifier = guidelineVerifier;
    }

    /**
     * 초안(draft)을 PromptSpec 기준으로 검증한다.
     * 전략 검증 후 규칙 기반 검증을 수행하고, HARD/FORBID 위반 시 실패로 병합한다.
     */
    public VerifyResult verify(String draft, PromptSpec spec) {
        VerifyResult strategyResult = objectiveRegistry
                .get(spec.getObjective())
                .verificationStrategy()
                .verify(new VerificationContext(spec, draft));

        if (guidelineVerifier == null || spec.getTaskDomain() == null) {
            return strategyResult;
        }
        var hardRules = spec.getTaskDomain().getRulesByLevel(RuleLevel.HARD);
        if (hardRules.isEmpty()) return strategyResult;

        GuidelineVerificationResult guidelineResult = guidelineVerifier.verify(draft, hardRules);
        if (guidelineResult.passed()) return strategyResult;

        List<String> reasons = new ArrayList<>(strategyResult.getFailureReasons());
        guidelineResult.failures().stream()
                .map(v -> v.ruleId() + ": " + v.message())
                .forEach(reasons::add);
        return VerifyResult.fail(strategyResult.getItemResults(), reasons);
    }
}
