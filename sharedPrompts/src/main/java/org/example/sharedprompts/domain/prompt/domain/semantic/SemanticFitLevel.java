package org.example.sharedprompts.domain.prompt.domain.semantic;

/**
 * Semantic strength for category–intent, role, action, or tone/style fit.
 * Replaces binary allowed/forbidden with explicit preference levels.
 *
 * <ul>
 *   <li>PREFERRED — natural fit; recommend by default.</li>
 *   <li>ALLOWED — valid; no warning.</li>
 *   <li>DISCOURAGED — valid but suboptimal; emit VALID_WITH_WARNING and hint.</li>
 *   <li>FORBIDDEN — invalid; validation fails.</li>
 * </ul>
 */
public enum SemanticFitLevel {

    /** Natural fit for this category+context; default recommendation. */
    PREFERRED,

    /** Valid; no warning. */
    ALLOWED,

    /** Valid but suboptimal; validation may add warning and recommendation hint. */
    DISCOURAGED,

    /** Invalid; validation fails. */
    FORBIDDEN;

    public boolean isAllowed() {
        return this != FORBIDDEN;
    }

    public boolean isValidWithWarning() {
        return this == DISCOURAGED;
    }

    public boolean isPreferredOrAllowed() {
        return this == PREFERRED || this == ALLOWED;
    }
}
