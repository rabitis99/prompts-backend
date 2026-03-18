package org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

import java.util.Optional;

/**
 * Bundle of validated policy documents for a single policy version.
 * Only created when validation passed (no errors). Used as input to binders.
 */
public interface ValidatedPolicyBundle {

    PolicyVersion policyVersion();

    Optional<RecommendationPreferencePolicyDocument> recommendationPreference();

    Optional<CompatibilityPolicyDocument> compatibility();

    Optional<ObjectivePolicyDocument> objective();

    Optional<RolePreferencePolicyDocument> rolePreference();

    Optional<RoleCompatibilityPolicyDocument> roleCompatibility();
}
