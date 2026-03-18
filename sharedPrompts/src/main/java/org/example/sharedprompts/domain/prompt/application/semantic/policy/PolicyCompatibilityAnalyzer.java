package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyChangeImpact;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

/**
 * Analyzes how a policy change affects recommendation behavior.
 * Heuristic-based: classifies changes as SAFE / BEHAVIOR_CHANGE / BREAKING and reports affected scope.
 */
public interface PolicyCompatibilityAnalyzer {

    /**
     * Analyze impact of moving from oldPolicy to newPolicy.
     */
    PolicyChangeImpact analyze(ValidatedPolicyBundle oldPolicy, ValidatedPolicyBundle newPolicy);
}
