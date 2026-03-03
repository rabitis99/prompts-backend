package org.example.sharedprompts.domain.prompt.domain.verification.soft;

import org.example.sharedprompts.domain.prompt.domain.verification.VerificationContext;
import org.example.sharedprompts.domain.prompt.domain.verification.base.BaseVerification;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;

import java.util.ArrayList;

/**
 * CREATIVE_WITH_CONSTRAINTS — Soft-verify: 형식·금지어·명백한 모순만 체크.
 * 의미적 창의성 판단, 스타일·톤 평가는 제외한다.
 *
 * <p><b>Rubric과의 관계:</b> 프로필의 {@link QualityRubric}에는 COVERAGE 등이 포함될 수 있으나,
 * 본 전략은 다음만 검증한다: FORMAT_COMPLIANCE, NO_PROHIBITED_CONTENT, NO_CONTRADICTION.
 * COVERAGE(요구사항·필수 섹션 커버리지)는 길이·심층성 판단이 필요하므로 soft 모드에서는
 * 의도적으로 검증하지 않으며, 이는 {@code PromptSpecValidatorTest#softVerifyDoesNotCheckCoverage}에서
 * 기대 동작으로 검증된다.
 */
public final class SoftVerification extends BaseVerification {

    @Override
    public VerifyResult verify(VerificationContext ctx) {
        var results = newResults();
        var failures = new ArrayList<String>();
        String draft = ctx.draft();
        var spec = ctx.spec();

        if (draft == null || draft.isBlank()) {
            failures.add("생성 결과가 비어 있습니다.");
            return buildResult(results, failures);
        }

        var contract = spec.getOutputContract();
        if (contract.hasJsonSchema()) {
            checkFormatCompliance(draft, contract, results, failures);
        }

        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        boolean noObviousContradiction = hasObviousContradiction(draft);
        results.put(QualityRubric.RubricItem.NO_CONTRADICTION, noObviousContradiction);
        if (!noObviousContradiction) {
            failures.add("명백한 논리 모순이 감지되었습니다.");
        }

        return buildResult(results, failures);
    }
}
