package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;

/**
 * Validates a policy document (structural + semantic).
 * Validators may depend on definition registries/catalogs for semantic checks.
 */
public interface PolicyDocumentValidator {

    /**
     * Policy type this validator handles (e.g. "RecommendationPreference").
     */
    String supportedPolicyType();

    /**
     * Validate document. Returns result with errors and/or warnings.
     * Semantic validation uses stable keys and registries; no enum names.
     */
    PolicyValidationResult validate(PolicyDocument document);
}
