package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;

import java.util.ArrayList;

/**
 * REASONING / PLANNING — Coverage + 모순 탐지 (표준).
 *
 * <p>생성자 파라미터로 Objective별 추가 체크를 주입받아 switch/case 없이 구성한다:
 * <ul>
 *   <li>REASONING: {@code checkUncertainty=true, checkInputPreservation=false}</li>
 *   <li>PLANNING:  {@code checkUncertainty=false, checkInputPreservation=true}</li>
 * </ul>
 */
public final class StandardVerification extends BaseVerification {

    private final boolean checkUncertainty;
    private final boolean checkInputPreservation;

    public StandardVerification(boolean checkUncertainty, boolean checkInputPreservation) {
        this.checkUncertainty = checkUncertainty;
        this.checkInputPreservation = checkInputPreservation;
    }

    @Override
    public VerifyResult verify(VerificationContext ctx) {
        var results = newResults();
        var failures = new ArrayList<String>();
        String draft = ctx.draft();
        var spec = ctx.spec();

        checkCoverage(draft, results, failures);

        if (checkUncertainty) {
            checkUncertaintyHandling(draft, results, failures);
        }
        if (checkInputPreservation) {
            checkInputPreservation(draft, spec.getClarifiedInput(), results, failures);
        }

        checkNoContradiction(draft, results, failures);
        checkNoProhibitedContent(draft, spec.getContentSandbox(), results, failures);

        return buildResult(results, failures);
    }
}
