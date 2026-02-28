package org.example.sharedprompts.domain.prompt.domain.verification;

import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;

public record VerificationContext(PromptSpec spec, String draft) {}
