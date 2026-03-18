package org.example.sharedprompts.domain.prompt.infrastructure.policy.validation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.RolePreferencePolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationError;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationWarning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structural + semantic validation for {@link RolePreferencePolicyDocument}.
 */
public final class RolePreferencePolicyDocumentValidator implements PolicyDocumentValidator {

    private static final String CONTEXT_KEY_PATTERN = ".+\\+.+";

    private final PolicyValidationContext context;

    public RolePreferencePolicyDocumentValidator(PolicyValidationContext context) {
        this.context = context != null ? context : new EmptyPolicyValidationContext();
    }

    @Override
    public String supportedPolicyType() {
        return RolePreferencePolicyDocument.POLICY_TYPE;
    }

    @Override
    public PolicyValidationResult validate(org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocument document) {
        if (!(document instanceof RolePreferencePolicyDocument doc)) {
            return PolicyValidationResult.failure(List.of(
                    new PolicyValidationError("INVALID_TYPE", "Expected RolePreferencePolicyDocument", "", document)
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

    private void structuralValidate(RolePreferencePolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        if (doc.policyVersion() == null || doc.policyVersion().isBlank()) {
            errors.add(new PolicyValidationError("MISSING_FIELD", "policyVersion is required", "policyVersion", null));
        }
        for (var e : doc.rules().entrySet()) {
            String ctx = e.getKey();
            if (ctx == null || ctx.isBlank()) {
                errors.add(new PolicyValidationError("BLANK_KEY", "Context key cannot be blank", "rules", ctx));
                continue;
            }
            if (!ctx.contains("+")) {
                errors.add(new PolicyValidationError("INVALID_CONTEXT_FORMAT", "Context key must contain CATEGORY+INTENT or CATEGORY+INTENT+actionKey", "rules." + ctx, ctx));
            }
            List<String> keys = e.getValue();
            if (keys == null) continue;
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                if (k == null || k.isBlank()) {
                    errors.add(new PolicyValidationError("BLANK_ROLE_KEY", "Role key cannot be blank", "rules." + ctx + "[" + i + "]", k));
                } else if (!seen.add(k.trim())) {
                    warnings.add(new PolicyValidationWarning("DUPLICATE_ROLE_PREFERENCE", "Duplicate role key in same context: " + k, "rules." + ctx));
                }
            }
        }
    }

    private void semanticValidate(RolePreferencePolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        Set<String> knownRoles = context.knownRoleKeys();
        if (knownRoles.isEmpty()) return; // no role registry: skip semantic check for role keys
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
            for (String roleKey : e.getValue() != null ? e.getValue() : List.<String>of()) {
                if (roleKey != null && !roleKey.isBlank() && !knownRoles.contains(roleKey.trim())) {
                    errors.add(new PolicyValidationError("UNKNOWN_ROLE_KEY", "Unknown role stable key: " + roleKey, "rules." + ctx, roleKey));
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
