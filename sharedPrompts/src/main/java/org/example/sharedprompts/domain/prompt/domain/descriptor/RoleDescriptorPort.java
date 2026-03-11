package org.example.sharedprompts.domain.prompt.domain.descriptor;

import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

/**
 * Port for role display/description text. Prefer this over calling
 * {@link RoleTypeInterface} display methods directly so that enums remain identity-only
 * and display is supplied by a dedicated provider.
 *
 * <p>Canonical API for "role name/description by locale"; implementations may delegate
 * to enum fields for backward compatibility until i18n/descriptor data is moved elsewhere.
 */
public interface RoleDescriptorPort {

    /**
     * Display name for the role in the given language.
     *
     * @param role role identity (nullable; returns empty string if null)
     * @param lang locale (null treated as default, e.g. KOREAN)
     * @return display name, never null
     */
    String getRoleName(RoleTypeInterface role, LanguageType lang);

    /**
     * Description for the role in the given language.
     *
     * @param role role identity (nullable; returns empty string if null)
     * @param lang locale (null treated as default)
     * @return description, may be empty
     */
    String getDescription(RoleTypeInterface role, LanguageType lang);
}
