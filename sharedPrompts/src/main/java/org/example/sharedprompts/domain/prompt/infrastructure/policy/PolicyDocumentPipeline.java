package org.example.sharedprompts.domain.prompt.infrastructure.policy;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentParser;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyLoadValidateBindResult;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyLoadValidateResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.DefaultValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationError;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationWarning;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pipeline: load (raw) → parse (document) → validate → bind (PolicyBundle).
 * Only validated documents are bound; invalid documents are never bound.
 */
public final class PolicyDocumentPipeline {

    private final PolicyDocumentLoader loader;
    private final List<PolicyDocumentParser> parsers;
    private final List<PolicyDocumentValidator> validators;
    private final PolicyBinder binder;

    public PolicyDocumentPipeline(
            PolicyDocumentLoader loader,
            List<PolicyDocumentParser> parsers,
            List<PolicyDocumentValidator> validators,
            PolicyBinder binder
    ) {
        this.loader = loader;
        this.parsers = parsers != null ? List.copyOf(parsers) : List.of();
        this.validators = validators != null ? List.copyOf(validators) : List.of();
        this.binder = binder;
    }

    /**
     * Load from classpath resource, parse as given policy type, validate, and optionally bind.
     * Returns result with validation outcome; bundle only if valid.
     */
    public PolicyLoadValidateBindResult loadValidateBind(
            String classpathResource,
            String policyType,
            PolicyVersion policyVersion
    ) {
        Map<String, Object> raw = loader.loadFromClasspath(classpathResource);
        return parseValidateBind(raw, policyType, policyVersion, Optional.of("classpath:" + classpathResource));
    }

    /**
     * Parse raw map, validate, and optionally bind. Use for in-memory or already-loaded content.
     */
    public PolicyLoadValidateBindResult parseValidateBind(
            Map<String, Object> raw,
            String policyType,
            PolicyVersion policyVersion,
            Optional<String> sourceInfo
    ) {
        PolicyDocument document = parseRaw(raw, policyType, sourceInfo);
        if (document == null) {
            return PolicyLoadValidateBindResult.failure(
                    policyVersion,
                    PolicyValidationResult.failure(List.of(
                            new PolicyValidationError("PARSE_FAILED", "No parser for policy type: " + policyType, "", raw)
                    ))
            );
        }
        PolicyValidationResult result = validate(document);
        if (!result.isBindable()) {
            return PolicyLoadValidateBindResult.failure(policyVersion, result);
        }
        ValidatedPolicyBundle validated = buildValidatedBundle(policyVersion, document, policyType);
        var bundle = binder.bind(validated);
        return PolicyLoadValidateBindResult.success(policyVersion, result, bundle);
    }

    /**
     * Build a full ValidatedPolicyBundle from multiple documents (e.g. one per family).
     * Validates each document; if any has errors, returns failure. Warnings only still allow bind.
     */
    public PolicyLoadValidateBindResult loadValidateBindBundle(
            PolicyVersion policyVersion,
            Map<String, Map<String, Object>> rawByPolicyType
    ) {
        List<PolicyValidationError> allErrors = new ArrayList<>();
        List<PolicyValidationWarning> allWarnings = new ArrayList<>();
        DefaultValidatedPolicyBundle.Builder builder = DefaultValidatedPolicyBundle.builder(policyVersion);

        for (var e : rawByPolicyType.entrySet()) {
            String policyType = e.getKey();
            PolicyDocument doc = parseRaw(e.getValue(), policyType, Optional.empty());
            if (doc == null) continue;
            PolicyValidationResult vr = validate(doc);
            allErrors.addAll(vr.getErrors());
            allWarnings.addAll(vr.getWarnings());
            if (vr.isBindable()) {
                addToBuilder(builder, doc, policyType);
            }
        }

        if (!allErrors.isEmpty()) {
            return PolicyLoadValidateBindResult.failure(
                    policyVersion,
                    PolicyValidationResult.failure(allErrors, allWarnings)
            );
        }
        ValidatedPolicyBundle validated = builder.build();
        var bundle = binder.bind(validated);
        return PolicyLoadValidateBindResult.success(
                policyVersion,
                new PolicyValidationResult(true, List.of(), allWarnings),
                bundle
        );
    }

    /**
     * Parse and validate multiple documents without binding.
     * Returns validated document bundle for versioned archive / diff.
     */
    public PolicyLoadValidateResult loadValidateBundle(
            PolicyVersion policyVersion,
            Map<String, Map<String, Object>> rawByPolicyType
    ) {
        List<PolicyValidationError> allErrors = new ArrayList<>();
        List<PolicyValidationWarning> allWarnings = new ArrayList<>();
        DefaultValidatedPolicyBundle.Builder builder = DefaultValidatedPolicyBundle.builder(policyVersion);

        for (var e : rawByPolicyType.entrySet()) {
            String policyType = e.getKey();
            PolicyDocument doc = parseRaw(e.getValue(), policyType, Optional.empty());
            if (doc == null) continue;
            PolicyValidationResult vr = validate(doc);
            allErrors.addAll(vr.getErrors());
            allWarnings.addAll(vr.getWarnings());
            if (vr.isBindable()) {
                addToBuilder(builder, doc, policyType);
            }
        }

        if (!allErrors.isEmpty()) {
            return PolicyLoadValidateResult.failure(
                    PolicyValidationResult.failure(allErrors, allWarnings)
            );
        }
        ValidatedPolicyBundle validated = builder.build();
        return PolicyLoadValidateResult.success(
                new PolicyValidationResult(true, List.of(), allWarnings),
                validated
        );
    }

    private PolicyDocument parseRaw(Map<String, Object> raw, String policyType, Optional<String> sourceInfo) {
        for (PolicyDocumentParser parser : parsers) {
            if (parser.supportedPolicyType().equals(policyType)) {
                return parser.parse(raw != null ? raw : Map.of());
            }
        }
        return null;
    }

    private PolicyValidationResult validate(PolicyDocument document) {
        for (PolicyDocumentValidator v : validators) {
            if (v.supportedPolicyType().equals(document.policyType())) {
                return v.validate(document);
            }
        }
        return PolicyValidationResult.success(List.of());
    }

    private void addToBuilder(DefaultValidatedPolicyBundle.Builder builder, PolicyDocument doc, String policyType) {
        if (doc instanceof RecommendationPreferencePolicyDocument d) {
            builder.recommendationPreference(d);
        } else if (doc instanceof CompatibilityPolicyDocument d) {
            builder.compatibility(d);
        } else if (doc instanceof ObjectivePolicyDocument d) {
            builder.objective(d);
        } else if (doc instanceof RolePreferencePolicyDocument d) {
            builder.rolePreference(d);
        } else if (doc instanceof RoleCompatibilityPolicyDocument d) {
            builder.roleCompatibility(d);
        }
    }

    private ValidatedPolicyBundle buildValidatedBundle(PolicyVersion policyVersion, PolicyDocument document, String policyType) {
        DefaultValidatedPolicyBundle.Builder builder = DefaultValidatedPolicyBundle.builder(policyVersion);
        addToBuilder(builder, document, policyType);
        return builder.build();
    }
}
