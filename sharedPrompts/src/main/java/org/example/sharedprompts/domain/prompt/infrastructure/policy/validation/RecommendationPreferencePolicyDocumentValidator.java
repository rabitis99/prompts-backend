package org.example.sharedprompts.domain.prompt.infrastructure.policy.validation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.RecommendationPreferencePolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationError;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationWarning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structural + semantic validation for {@link RecommendationPreferencePolicyDocument}.
 */
public final class RecommendationPreferencePolicyDocumentValidator implements PolicyDocumentValidator {

    private static final String CONTEXT_KEY_PATTERN = ".+\\+.+"; // at least CATEGORY+INTENT

    private final PolicyValidationContext context;

    public RecommendationPreferencePolicyDocumentValidator(PolicyValidationContext context) {
        this.context = context != null ? context : new EmptyPolicyValidationContext();
    }

    @Override
    public String supportedPolicyType() {
        return RecommendationPreferencePolicyDocument.POLICY_TYPE;
    }

    @Override
    public PolicyValidationResult validate(org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.PolicyDocument document) {
        if (!(document instanceof RecommendationPreferencePolicyDocument doc)) {
            return PolicyValidationResult.failure(List.of(
                    new PolicyValidationError("INVALID_TYPE", "Expected RecommendationPreferencePolicyDocument", "", document)
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

    private void structuralValidate(RecommendationPreferencePolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
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
                errors.add(new PolicyValidationError("INVALID_CONTEXT_FORMAT", "Context key must be CATEGORY+INTENT (e.g. WRITING+GENERATE)", "rules." + ctx, ctx));
            }
            List<String> keys = e.getValue();
            if (keys == null) {
                errors.add(new PolicyValidationError("NULL_VALUE", "Preferred action list cannot be null", "rules." + ctx, null));
                continue;
            }
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < keys.size(); i++) {
                String k = keys.get(i);
                if (k == null || k.isBlank()) {
                    errors.add(new PolicyValidationError("BLANK_ACTION_KEY", "Action key cannot be blank", "rules." + ctx + "[" + i + "]", k));
                } else if (!seen.add(k.trim())) {
                    warnings.add(new PolicyValidationWarning("DUPLICATE_PREFERENCE", "Duplicate action key in same context: " + k, "rules." + ctx));
                }
            }
        }
    }

    private void semanticValidate(RecommendationPreferencePolicyDocument doc, List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        Set<String> knownActions = context.knownActionKeys();
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
            for (String actionKey : e.getValue()) {
                if (actionKey == null || actionKey.isBlank()) continue;
                if (!knownActions.contains(actionKey.trim())) {
                    errors.add(new PolicyValidationError("UNKNOWN_ACTION_KEY", "Unknown action stable key: " + actionKey, "rules." + ctx, actionKey));
                }
            }
        }
    }

    private static final class EmptyPolicyValidationContext implements PolicyValidationContext {
        @Override
        public Set<String> knownCategoryKeys() { return Set.of(); }
        @Override
        public Set<String> knownIntentKeys() { return Set.of(); }
        @Override
        public Set<String> knownActionKeys() { return Set.of(); }
        @Override
        public Set<String> knownActionGroupKeys() { return Set.of(); }
        @Override
        public Set<String> knownRoleKeys() { return Set.of(); }
        @Override
        public Set<String> knownObjectiveNames() { return Set.of(); }
        @Override
        public Set<String> knownTaskDomainNames() { return Set.of(); }
    }
}
