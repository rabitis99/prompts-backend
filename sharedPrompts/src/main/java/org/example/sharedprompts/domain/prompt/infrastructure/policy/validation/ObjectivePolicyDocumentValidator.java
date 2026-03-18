package org.example.sharedprompts.domain.prompt.infrastructure.policy.validation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.ObjectivePolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationError;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationWarning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Structural + semantic validation for {@link ObjectivePolicyDocument}.
 */
public final class ObjectivePolicyDocumentValidator implements PolicyDocumentValidator {

    private final PolicyValidationContext context;

    public ObjectivePolicyDocumentValidator(PolicyValidationContext context) {
        this.context = context != null ? context : new EmptyPolicyValidationContext();
    }

    @Override
    public String supportedPolicyType() {
        return ObjectivePolicyDocument.POLICY_TYPE;
    }

    @Override
    public PolicyValidationResult validate(org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocument document) {
        if (!(document instanceof ObjectivePolicyDocument doc)) {
            return PolicyValidationResult.failure(List.of(
                    new PolicyValidationError("INVALID_TYPE", "Expected ObjectivePolicyDocument", "", document)
            ));
        }
        List<PolicyValidationError> errors = new ArrayList<>();
        List<PolicyValidationWarning> warnings = new ArrayList<>();

        structuralValidate(doc, errors, warnings);
        if (errors.isEmpty()) {
            semanticValidate(doc, errors, warnings);
        }

        return errors.isEmpty()
                ? PolicyValidationResult.success(warnings)
                : PolicyValidationResult.failure(errors, warnings);
    }

    private void structuralValidate(ObjectivePolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        if (doc.policyVersion() == null || doc.policyVersion().isBlank()) {
            errors.add(new PolicyValidationError("MISSING_FIELD", "policyVersion is required", "policyVersion", null));
        }
        for (var e : doc.explicitMappings().entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) {
                errors.add(new PolicyValidationError("BLANK_ACTION_KEY", "Action key cannot be blank in explicitMappings", "explicitMappings", e.getKey()));
            }
            if (e.getValue() == null || e.getValue().isBlank()) {
                errors.add(new PolicyValidationError("BLANK_OBJECTIVE", "Objective name cannot be blank", "explicitMappings." + e.getKey(), e.getValue()));
            }
        }
        for (var e : doc.domainDefaults().entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) {
                errors.add(new PolicyValidationError("BLANK_TASK_DOMAIN", "Task domain key cannot be blank in domainDefaults", "domainDefaults", e.getKey()));
            }
            if (e.getValue() == null || e.getValue().isBlank()) {
                errors.add(new PolicyValidationError("BLANK_OBJECTIVE", "Objective name cannot be blank in domainDefaults", "domainDefaults." + e.getKey(), e.getValue()));
            }
        }
    }

    private void semanticValidate(ObjectivePolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        Set<String> knownActions = context.knownActionKeys();
        Set<String> knownObjectives = context.knownObjectiveNames();
        Set<String> knownDomains = context.knownTaskDomainNames();
        for (var e : doc.explicitMappings().entrySet()) {
            String actionKey = e.getKey();
            if (actionKey != null && !actionKey.isBlank() && !knownActions.contains(actionKey.trim())) {
                errors.add(new PolicyValidationError("UNKNOWN_ACTION_KEY", "Unknown action stable key: " + actionKey, "explicitMappings", actionKey));
            }
            String obj = e.getValue();
            if (obj != null && !obj.isBlank() && !knownObjectives.contains(obj.trim())) {
                errors.add(new PolicyValidationError("UNKNOWN_OBJECTIVE", "Unknown objective name: " + obj, "explicitMappings." + actionKey, obj));
            }
        }
        for (var e : doc.domainDefaults().entrySet()) {
            String domainKey = e.getKey();
            if (domainKey != null && !domainKey.isBlank() && !knownDomains.contains(domainKey.trim())) {
                errors.add(new PolicyValidationError("UNKNOWN_TASK_DOMAIN", "Unknown task domain: " + domainKey, "domainDefaults", domainKey));
            }
            String obj = e.getValue();
            if (obj != null && !obj.isBlank() && !knownObjectives.contains(obj.trim())) {
                errors.add(new PolicyValidationError("UNKNOWN_OBJECTIVE", "Unknown objective name: " + obj, "domainDefaults." + domainKey, obj));
            }
        }
    }

    private static final class EmptyPolicyValidationContext implements PolicyValidationContext {
        @Override public Set<String> knownCategoryKeys() { return Set.of(); }
        @Override public Set<String> knownIntentKeys() { return Set.of(); }
        @Override public Set<String> knownActionKeys() { return Set.of(); }
        @Override public Set<String> knownActionGroupKeys() { return Set.of(); }
        @Override public Set<String> knownRoleKeys() { return Set.of(); }
        @Override public Set<String> knownObjectiveNames() { return Set.of(); }
        @Override public Set<String> knownTaskDomainNames() { return Set.of(); }
    }
}
