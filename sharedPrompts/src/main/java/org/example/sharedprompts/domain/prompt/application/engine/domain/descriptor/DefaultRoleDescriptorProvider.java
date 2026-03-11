package org.example.sharedprompts.domain.prompt.application.engine.domain.descriptor;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.descriptor.RoleDescriptorPort;
import org.springframework.stereotype.Component;

/** 기본 역할 설명 제공자 */
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