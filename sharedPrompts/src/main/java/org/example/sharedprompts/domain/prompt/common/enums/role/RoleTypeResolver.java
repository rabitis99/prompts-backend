package org.example.sharedprompts.domain.prompt.common.enums.role;

import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.BusinessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.CustomerSupportRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.content_creation.ContentCreationRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.creative.CreativeRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.design.DesignRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.AiMlRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.CybersecurityRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.DevelopmentRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.education.EducationRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.legal.LegalRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.EtcRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.HealthFitnessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.SocialRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.marketing.MarketingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.productivity.ProductivityRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.research.ResearchRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.core.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumCompatParser;

import java.util.List;
import java.util.Optional;

import static org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumCompatParser.Mode.LENIENT;

/**
 * Resolves role stable key to {@link RoleTypeInterface}. Used when building role candidates
 * from policy (e.g. {@link org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource}).
 * Does not throw; returns empty when key is unknown or blank.
 */
public final class RoleTypeResolver {

    private static final List<Class<? extends Enum<?>>> ROLE_TYPE_ENUMS = List.of(
            CoreRoleType.class,
            ProductivityRoleType.class,
            DevelopmentRoleType.class,
            AiMlRoleType.class,
            CybersecurityRoleType.class,
            MarketingRoleType.class,
            ContentCreationRoleType.class,
            CreativeRoleType.class,
            EducationRoleType.class,
            LegalRoleType.class,
            ResearchRoleType.class,
            BusinessRoleType.class,
            CustomerSupportRoleType.class,
            DesignRoleType.class,
            WritingRoleType.class,
            EtcRoleType.class,
            HealthFitnessRoleType.class,
            SocialRoleType.class
    );

    private RoleTypeResolver() {}

    /**
     * Resolves a role stable key to a role type. Returns empty if key is null, blank, or unknown.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RoleTypeInterface> resolveOptional(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String trimmed = key.trim();
        for (Class<? extends Enum<?>> enumClass : ROLE_TYPE_ENUMS) {
            Enum<?> parsed = EnumCompatParser.parse(trimmed, (Class) enumClass, LENIENT);
            if (parsed instanceof RoleTypeInterface) {
                return Optional.of((RoleTypeInterface) parsed);
            }
        }
        return Optional.empty();
    }
}
