package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;

import java.util.ArrayList;

/**
 * CREATIVE_WITH_CONSTRAINTS — Soft-verify: 형식·금지어·명백한 모순만 체크.
 * 의미적 창의성 판단, 스타일·톤 평가는 제외한다.
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

        boolean noObviousContradiction = !hasObviousContradiction(draft);
        results.put(QualityRubric.RubricItem.NO_CONTRADICTION, noObviousContradiction);
        if (!noObviousContradiction) {
            failures.add("명백한 논리 모순이 감지되었습니다.");
        }

        return buildResult(results, failures);
    }
}
