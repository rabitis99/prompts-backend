package org.example.sharedprompts.domain.prompt.common.contract;

/**
 * Stable, serialization-safe enum identifier contract.
 *
 * <p><b>Policy:</b> Enums exposed through API, persistence, or serialization contracts
 * should implement StableKeyedEnum unless they are intentionally internal-only. This keeps
 * external keys stable across refactors and renames and avoids breaking clients and stored data.
 *
 * <p>Implementations must provide a {@link #key()} value that is:
 * <ul>
 *   <li>stable over time (never changed or reused)</li>
 *   <li>suitable for use as a persistent identifier (API, DB, config, logs)</li>
 *   <li>independent from {@link Enum#name()}, which may still change for refactors</li>
 * </ul>
 *
 * <p>Guidelines for implementors:
 * <ul>
 *   <li>Use an uppercase, dot-separated scheme such as {@code "TONE.FRIENDLY"} or
 *       {@code "PROMPT_CATEGORY.PRODUCTIVITY"}.</li>
 *   <li>Once published, never modify or remove an existing key value.</li>
 *   <li>When deprecating enum constants, keep their keys for backward compatibility.</li>
 * </ul>
 */
public interface StableKeyedEnum {

    /**
     * Returns the stable, never-changing identifier for this enum constant.
     *
     * <p>This value is intended to be used as the canonical serialized representation
     * across services and storage layers. It must remain stable forever, even if the
     * enum constant name changes.</p>
     */
    String key();
}
