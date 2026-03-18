package org.example.sharedprompts.domain.prompt.infrastructure.policy.binding;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.DefaultRolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.RolePreferencePolicyDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds validated {@link RolePreferencePolicyDocument} to runtime {@link RolePreferenceSource}.
 */
public final class RolePreferencePolicyBinder {

    public RolePreferenceSource bind(RolePreferencePolicyDocument document) {
        if (document == null) {
            return new DefaultRolePreferenceSource(Map.of());
        }
        Map<String, List<String>> rules = new LinkedHashMap<>(document.rules());
        return new DefaultRolePreferenceSource(rules);
    }
}
