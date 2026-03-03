package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

public record VerificationContext(PromptSpec spec, String draft) {}
