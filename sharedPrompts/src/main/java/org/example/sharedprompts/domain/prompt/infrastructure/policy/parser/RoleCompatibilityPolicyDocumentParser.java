package org.example.sharedprompts.domain.prompt.infrastructure.policy.parser;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentParser;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocumentMetadata;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.RoleCompatibilityPolicyDocument;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses raw map into {@link RoleCompatibilityPolicyDocument}.
 */
public final class RoleCompatibilityPolicyDocumentParser implements PolicyDocumentParser {

    @Override
    public String supportedPolicyType() {
        return RoleCompatibilityPolicyDocument.POLICY_TYPE;
    }

    @Override
    public RoleCompatibilityPolicyDocument parse(Map<String, Object> raw) {
        if (raw == null) raw = Map.of();
        String policyVersion = PolicyDocumentParserSupport.stringOr(raw, "policyVersion", "unknown");
        String policyType = PolicyDocumentParserSupport.stringOr(raw, "policyType", RoleCompatibilityPolicyDocument.POLICY_TYPE);
        PolicyDocumentMetadata metadata = PolicyDocumentParserSupport.metadata(raw);
        Optional<String> sourceInfo = PolicyDocumentParserSupport.sourceInfo(raw);
        Map<String, List<String>> rules = PolicyDocumentParserSupport.rulesOrPreferences(raw);
        return new RoleCompatibilityPolicyDocument(
                policyVersion,
                policyType,
                metadata,
                sourceInfo,
                rules
        );
    }
}
