package org.example.sharedprompts.domain.prompt.infrastructure.policy.parser;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentParser;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.CompatibilityPolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocumentMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses raw map into {@link CompatibilityPolicyDocument}.
 */
public final class CompatibilityPolicyDocumentParser implements PolicyDocumentParser {

    @Override
    public String supportedPolicyType() {
        return CompatibilityPolicyDocument.POLICY_TYPE;
    }

    @Override
    public CompatibilityPolicyDocument parse(Map<String, Object> raw) {
        if (raw == null) raw = Map.of();
        String policyVersion = PolicyDocumentParserSupport.stringOr(raw, "policyVersion", "unknown");
        String policyType = PolicyDocumentParserSupport.stringOr(raw, "policyType", CompatibilityPolicyDocument.POLICY_TYPE);
        PolicyDocumentMetadata metadata = PolicyDocumentParserSupport.metadata(raw);
        Optional<String> sourceInfo = PolicyDocumentParserSupport.sourceInfo(raw);
        Map<String, List<String>> rules = PolicyDocumentParserSupport.rulesOrPreferences(raw);
        return new CompatibilityPolicyDocument(
                policyVersion,
                policyType,
                metadata,
                sourceInfo,
                rules
        );
    }
}
