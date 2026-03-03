package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.result.VerifyResult;

@FunctionalInterface
public interface VerificationStrategy {
    VerifyResult verify(VerificationContext ctx);
}
