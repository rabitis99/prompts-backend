package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyDiff;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

/**
 * Computes structural diff between two policy document sets.
 * Document-based; uses stable rule identifiers (e.g. context key, mapping key).
 */
public interface PolicyDiffService {

    /**
     * Compute diff from older to newer validated bundle.
     * Rule changes are keyed by policyFamily and ruleIdentifier.
     */
    PolicyDiff diff(ValidatedPolicyBundle from, ValidatedPolicyBundle to);
}
