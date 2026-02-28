package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;

@FunctionalInterface
public interface VerificationStrategy {
    VerifyResult verify(VerificationContext ctx);
}
