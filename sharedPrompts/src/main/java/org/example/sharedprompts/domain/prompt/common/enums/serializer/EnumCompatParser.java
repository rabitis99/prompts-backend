package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

/**
 * Compatibility parser for enums that supports both legacy {@link Enum#name()}
 * values and new stable {@link StableKeyedEnum#key()} identifiers.
 *
 * <p>This allows us to:
 * <ul>
 *   <li>continue accepting existing serialized enum names (backward compatible)</li>
 *   <li>start accepting stable keys as an alternative input format</li>
 *   <li>gradually migrate external systems to use stable keys without breaking APIs</li>
 * </ul>
 */
public final class EnumCompatParser {

    private EnumCompatParser() {
    }

    public enum Mode {
        /**
         * Accept both legacy enum names and stable keys.
         * Unknown values result in {@code null}.
         */
        LENIENT,

        /**
         * Accept both legacy enum names and stable keys.
         * Unknown values result in {@link IllegalArgumentException}.
         */
        STRICT
    }

    /**
     * Parse using {@link Mode#LENIENT}.
     */
    public static <E extends Enum<E>> E parse(String value, Class<E> enumClass) {
        return parse(value, enumClass, Mode.LENIENT);
    }

    /**
     * Parse an enum from either its legacy {@link Enum#name()} or its stable key.
     *
     * @param value     serialized enum representation (name or key)
     * @param enumClass enum type
     * @param mode      strictness mode for unknown values
     * @return resolved enum constant, or null in lenient mode when not found
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E parse(String value, Class<E> enumClass, Mode mode) {
        if (enumClass == null) {
            throw new IllegalArgumentException("enumClass must not be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (value == null || value.isBlank()) {
            if (mode == Mode.LENIENT) {
                return null;
            }
            throw new IllegalArgumentException("value must not be null/blank");
        }

        // 1) Try legacy Enum.name() first for backward compatibility
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ignored) {
        }

        // 2) If enum implements StableKeyedEnum, try matching by key
        if (StableKeyedEnum.class.isAssignableFrom(enumClass)) {
            for (E constant : enumClass.getEnumConstants()) {
                StableKeyedEnum keyed = (StableKeyedEnum) constant;
                if (value.equals(keyed.key())) {
                    return constant;
                }
            }
        }

        if (mode == Mode.LENIENT) {
            return null;
        }
        throw new IllegalArgumentException(
                "Unknown enum value '" + value + "' for type " + enumClass.getName());
    }
}

