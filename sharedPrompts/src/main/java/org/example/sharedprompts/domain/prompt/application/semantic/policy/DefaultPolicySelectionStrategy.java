package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.application.semantic.experiment.ExperimentContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

/**
 * Always returns a single active policy version.
 * No experiment routing; suitable when policy version is fixed (e.g. from config).
 */
public final class DefaultPolicySelectionStrategy implements PolicySelectionStrategy {

    private final PolicyVersion activeVersion;

    public DefaultPolicySelectionStrategy(PolicyVersion activeVersion) {
        this.activeVersion = activeVersion;
    }

    @Override
    public PolicyVersion selectPolicyVersion(ExperimentContext requestContext) {
        return activeVersion;
    }
}
