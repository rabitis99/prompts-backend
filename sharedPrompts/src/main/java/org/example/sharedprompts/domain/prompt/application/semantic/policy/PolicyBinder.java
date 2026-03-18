package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

/**
 * Binds a validated policy bundle to runtime policy sources (PolicyBundle).
 * Only invoked when validation has passed (no errors).
 */
public interface PolicyBinder {

    /**
     * Produce runtime PolicyBundle from validated documents.
     * ValidatedPolicyBundle must be valid (isBindable() == true).
     */
    PolicyBundle bind(ValidatedPolicyBundle validated);
}
