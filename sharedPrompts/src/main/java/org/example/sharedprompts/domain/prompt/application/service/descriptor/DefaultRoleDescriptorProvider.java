package org.example.sharedprompts.domain.prompt.application.service.descriptor;

import org.example.sharedprompts.domain.prompt.domain.descriptor.RoleDescriptorPort;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.springframework.stereotype.Component;

/**
 * Default role descriptor: delegates to {@link RoleTypeInterface} display methods.
 * Keeps display data on enums for now; callers use this port so the primary API is
 * the port, not the enum.
 */
@Component
public class DefaultRoleDescriptorProvider implements RoleDescriptorPort {

    @Override
    public String getRoleName(RoleTypeInterface role, LanguageType lang) {
        if (role == null) {
            return "";
        }
        return role.getRoleNameByLang(lang != null ? lang : LanguageType.KOREAN);
    }

    @Override
    public String getDescription(RoleTypeInterface role, LanguageType lang) {
        if (role == null) {
            return "";
        }
        return role.getDescriptionByLang(lang != null ? lang : LanguageType.KOREAN);
    }
}
