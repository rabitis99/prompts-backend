package org.example.sharedprompts.domain.prompt.infrastructure.policy.parser;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentParser;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.ObjectivePolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocumentMetadata;

import java.util.Map;
import java.util.Optional;

/**
 * Parses raw map into {@link ObjectivePolicyDocument}.
 * Expects "explicitMappings" (actionKey -> objective name) and "domainDefaults" (taskDomain -> objective name).
 */
public final class ObjectivePolicyDocumentParser implements PolicyDocumentParser {

    @Override
    public String supportedPolicyType() {
        return ObjectivePolicyDocument.POLICY_TYPE;
    }

    @Override
    public ObjectivePolicyDocument parse(Map<String, Object> raw) {
        if (raw == null) raw = Map.of();
        String policyVersion = PolicyDocumentParserSupport.stringOr(raw, "policyVersion", "unknown");
        String policyType = PolicyDocumentParserSupport.stringOr(raw, "policyType", ObjectivePolicyDocument.POLICY_TYPE);
        PolicyDocumentMetadata metadata = PolicyDocumentParserSupport.metadata(raw);
        Optional<String> sourceInfo = PolicyDocumentParserSupport.sourceInfo(raw);
        Map<String, String> explicitMappings = PolicyDocumentParserSupport.stringMap(raw, "explicitMappings");
        Map<String, String> domainDefaults = PolicyDocumentParserSupport.stringMap(raw, "domainDefaults");
        return new ObjectivePolicyDocument(
                policyVersion,
                policyType,
                metadata,
                sourceInfo,
                explicitMappings,
                domainDefaults
        );
    }
}
