package org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema;

import java.util.Map;
import java.util.Optional;

/**
 * Schema/DSL document for objective mapping policy.
 * Entries: action stable key → objective key (e.g. "REASONING", "ANALYTICAL").
 * Domain defaults: task domain key → objective key.
 */
public record ObjectivePolicyDocument(
        String policyVersion,
        String policyType,
        PolicyDocumentMetadata metadata,
        Optional<String> sourceInfo,
        Map<String, String> explicitMappings,
        Map<String, String> domainDefaults
) implements PolicyDocument {

    public static final String POLICY_TYPE = "Objective";

    public ObjectivePolicyDocument {
        policyType = policyType != null ? policyType : POLICY_TYPE;
        metadata = metadata != null ? metadata : PolicyDocumentMetadata.empty();
        sourceInfo = sourceInfo != null ? sourceInfo : Optional.empty();
        explicitMappings = explicitMappings != null ? Map.copyOf(explicitMappings) : Map.of();
        domainDefaults = domainDefaults != null ? Map.copyOf(domainDefaults) : Map.of();
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
