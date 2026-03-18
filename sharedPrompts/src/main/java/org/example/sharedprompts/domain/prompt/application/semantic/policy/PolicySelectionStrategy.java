package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.application.semantic.experiment.ExperimentContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

/**
 * Selects which policy version to use for a recommendation request.
 * Enables single active version (default) or experiment-based variant selection.
 */
@FunctionalInterface
public interface PolicySelectionStrategy {

    /**
     * Select the policy version for this request.
     * When no experiment context is provided, returns the default/active version.
     */
    PolicyVersion selectPolicyVersion(ExperimentContext requestContext);
}
