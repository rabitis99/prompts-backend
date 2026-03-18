package org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Schema/DSL document for role preference policy.
 * Rules: context key ("CATEGORY+INTENT" or "CATEGORY+INTENT+actionKey") → ordered preferred role stable keys.
 */
public record RolePreferencePolicyDocument(
        String policyVersion,
        String policyType,
        PolicyDocumentMetadata metadata,
        Optional<String> sourceInfo,
        Map<String, List<String>> rules
) implements PolicyDocument {

    public static final String POLICY_TYPE = "RolePreference";

    public RolePreferencePolicyDocument {
        policyType = policyType != null ? policyType : POLICY_TYPE;
        metadata = metadata != null ? metadata : PolicyDocumentMetadata.empty();
        sourceInfo = sourceInfo != null ? sourceInfo : Optional.empty();
        rules = rules != null ? Map.copyOf(rules) : Map.of();
    }

    @Override
    public String policyVersion() {
        return policyVersion;
    }

    @Override
    public String policyType() {
        return policyType;
    }

    @Override
    public PolicyDocumentMetadata metadata() {
        return metadata;
    }

    @Override
    public Optional<String> sourceInfo() {
        return sourceInfo;
    }
}
