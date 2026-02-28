package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;

import java.util.ArrayList;

/**
 * FACTUAL / ANALYTICAL — Chain-of-Verification: 사실·수치·인용 검증, CITE_OR_UNCERTAIN 강화.
 */
public final class ChainOfVerification extends BaseVerification {

    @Override
    public VerifyResult verify(VerificationContext ctx) {
        var results = newResults();
        var failures = new ArrayList<String>();
        String draft = ctx.draft();
        var spec = ctx.spec();

        checkCoverage(draft, results, failures);
        checkInputPreservation(draft, spec.getClarifiedInput(), results, failures);
        checkNoContradiction(draft, results, failures);
        checkUncertaintyHandling(draft, results, failures);
        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        return buildResult(results, failures);
    }
}
