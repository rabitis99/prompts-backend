package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocument;

import java.util.Map;

/**
 * Parses raw document (map) into a typed policy document (schema/DSL).
 * Does not validate; parsing may produce invalid documents.
 */
public interface PolicyDocumentParser {

    /**
     * Supported policy type (e.g. "RecommendationPreference", "Compatibility").
     */
    String supportedPolicyType();

    /**
     * Parse raw map into a policy document. Throws or returns invalid document on format error.
     */
    PolicyDocument parse(Map<String, Object> raw);
}
