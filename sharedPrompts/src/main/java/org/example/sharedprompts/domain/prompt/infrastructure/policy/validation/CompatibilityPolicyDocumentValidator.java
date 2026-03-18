package org.example.sharedprompts.domain.prompt.infrastructure.policy.validation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.CompatibilityPolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationError;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationWarning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structural + semantic validation for {@link CompatibilityPolicyDocument}.
 */
public final class CompatibilityPolicyDocumentValidator implements PolicyDocumentValidator {

    private static final String CONTEXT_KEY_PATTERN = ".+\\+.+";

    private final PolicyValidationContext context;

    public CompatibilityPolicyDocumentValidator(PolicyValidationContext context) {
        this.context = context != null ? context : new EmptyPolicyValidationContext();
    }

    @Override
    public String supportedPolicyType() {
        return CompatibilityPolicyDocument.POLICY_TYPE;
    }

    @Override
    public PolicyValidationResult validate(org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocument document) {
        if (!(document instanceof CompatibilityPolicyDocument doc)) {
            return PolicyValidationResult.failure(List.of(
                    new PolicyValidationError("INVALID_TYPE", "Expected CompatibilityPolicyDocument", "", document)
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

    private void structuralValidate(CompatibilityPolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        if (doc.policyVersion() == null || doc.policyVersion().isBlank()) {
            errors.add(new PolicyValidationError("MISSING_FIELD", "policyVersion is required", "policyVersion", null));
        }
        for (var e : doc.rules().entrySet()) {
            String ctx = e.getKey();
            if (ctx == null || ctx.isBlank()) {
                errors.add(new PolicyValidationError("BLANK_KEY", "Context key cannot be blank", "rules", ctx));
                continue;
            }
            if (!ctx.matches(CONTEXT_KEY_PATTERN)) {
                errors.add(new PolicyValidationError("INVALID_CONTEXT_FORMAT", "Context key must be CATEGORY+INTENT", "rules." + ctx, ctx));
            }
            for (String groupKey : e.getValue() != null ? e.getValue() : List.<String>of()) {
                if (groupKey == null || groupKey.isBlank()) {
                    errors.add(new PolicyValidationError("BLANK_GROUP_KEY", "Action group key cannot be blank", "rules." + ctx, groupKey));
                }
            }
        }
    }

    private void semanticValidate(CompatibilityPolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        Set<String> knownGroups = context.knownActionGroupKeys();
        Set<String> knownCategories = context.knownCategoryKeys();
        Set<String> knownIntents = context.knownIntentKeys();
        for (var e : doc.rules().entrySet()) {
            String ctx = e.getKey();
            String[] parts = ctx.split("\\+", -1);
            if (parts.length >= 2) {
                String catPart = parts[0].trim();
                String intentPart = parts[1].trim();
                if (!catPart.isEmpty() && !knownCategories.contains(catPart)) {
                    errors.add(new PolicyValidationError("UNKNOWN_CATEGORY", "Unknown category key: " + catPart, "rules." + ctx, catPart));
                }
                if (!intentPart.isEmpty() && !knownIntents.contains(intentPart)) {
                    errors.add(new PolicyValidationError("UNKNOWN_INTENT", "Unknown intent key: " + intentPart, "rules." + ctx, intentPart));
                }
            }
            if (e.getValue() != null) {
                for (String groupKey : e.getValue()) {
                    if (groupKey != null && !groupKey.isBlank() && !knownGroups.contains(groupKey.trim())) {
                        errors.add(new PolicyValidationError("UNKNOWN_ACTION_GROUP", "Unknown action group key: " + groupKey, "rules." + ctx, groupKey));
                    }
                }
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
