package org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema;

import java.util.Optional;

/**
 * Base contract for a policy document (declarative schema/DSL).
 * Not the runtime policy object; used for load → parse → validate → bind.
 */
public interface PolicyDocument {

    /**
     * Policy version identifier (e.g. "2026-03-recommendation-v1").
     */
    String policyVersion();

    /**
     * Policy type (e.g. "RecommendationPreference", "Compatibility", "Objective", "RolePreference", "RoleCompatibility").
     */
    String policyType();

    /**
     * Optional metadata (description, sourceType, experimentTag).
     */
    PolicyDocumentMetadata metadata();

    /**
     * Optional source identifier (e.g. "classpath:policy/recommendation.json").
     */
    Optional<String> sourceInfo();
}
