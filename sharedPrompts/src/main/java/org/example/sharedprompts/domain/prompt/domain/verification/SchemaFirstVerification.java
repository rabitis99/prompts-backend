package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;

import java.util.ArrayList;

/**
 * EXTRACTION — JSON Schema 기반 검증 우선, 통과 시 LLM Verify 생략 가능.
 */
public final class SchemaFirstVerification extends BaseVerification {

    @Override
    public VerifyResult verify(VerificationContext ctx) {
        var results = newResults();
        var failures = new ArrayList<String>();
        String draft = ctx.draft();
        var spec = ctx.spec();

        checkFormatCompliance(draft, spec.getOutputContract(), results, failures);
        checkInputPreservation(draft, spec.getClarifiedInput(), results, failures);
        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        return buildResult(results, failures);
    }
}
