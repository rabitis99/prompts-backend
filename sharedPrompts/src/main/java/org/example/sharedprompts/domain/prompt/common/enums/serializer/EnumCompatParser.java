package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.global.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Compatibility parser for enums that supports both legacy {@link Enum#name()}
 * values and new stable {@link StableKeyedEnum#key()} identifiers.
 *
 * <p>This is <b>deserialization/compatibility infrastructure only</b>. It allows:
 * <ul>
 *   <li>continuing to accept existing serialized enum names (backward compatible)</li>
 *   <li>accepting stable keys as an alternative input format</li>
 *   <li>gradual migration of external systems to stable keys without breaking APIs</li>
 * </ul>
 *
 * <p>Parsing order is deterministic: legacy {@link Enum#name()} first, then
 * {@link StableKeyedEnum#key()}. Input is trimmed before lookup; blank/null
 * is rejected in STRICT mode and yields null in LENIENT mode.
 */
public final class EnumCompatParser {

    /**
     * Per-enum-class index from stable key to enum constant.
     * Built lazily; each snapshot is immutable. Duplicate stable keys
     * within an enum class cause immediate failure at index build time.
     */
    private static final ConcurrentMap<Class<?>, Map<String, ? extends Enum<?>>> KEY_INDEX =
            new ConcurrentHashMap<>();

    private EnumCompatParser() {
    }

    public enum Mode {
        /**
         * Accept both legacy enum names and stable keys.
         * Unknown or blank/null values result in {@code null}.
         */
        LENIENT,

        /**
         * Accept both legacy enum names and stable keys.
         * Unknown or blank/null values result in {@link IllegalArgumentException}.
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
     * <p>Order: (1) legacy name lookup, (2) stable key lookup if the enum
     * implements {@link StableKeyedEnum}. Unknown value: null in LENIENT,
     * exception in STRICT.
     *
     * @param value     serialized enum representation (name or key); trimmed before lookup
     * @param enumClass enum type (must be a real enum class)
     * @param mode      strictness for unknown/blank values
     * @return resolved enum constant, or null in LENIENT when not found or blank/null
     */
    public static <E extends Enum<E>> E parse(String value, Class<E> enumClass, Mode mode) {
        validateEnumClass(enumClass);
        validateMode(mode);

        String normalized = normalizeValue(value);
        if (normalized == null) {
            return handleBlankOrNull(enumClass, mode);
        }

        // 1) Legacy Enum.name() first — deterministic backward compatibility
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException ignored) {
            // Not a legacy name; proceed to stable key lookup
        }

        // 2) Stable key lookup second — only for StableKeyedEnum types
        if (StableKeyedEnum.class.isAssignableFrom(enumClass)) {
            Map<String, E> index = getOrBuildStableKeyIndex(enumClass);
            E matched = index.get(normalized);
            if (matched != null) {
                return matched;
            }
        }

        if (mode == Mode.LENIENT) {
            return null;
        }
        throw unknownValueException(normalized, enumClass, mode);
    }

    private static void validateEnumClass(Class<?> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("enumClass must not be null");
        }
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException(
                    "enumClass must be an enum type: " + enumClass.getName());
        }
    }

    private static void validateMode(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
    }

    /**
     * Returns trimmed value, or null if input is null or blank after trim.
     * Same normalization is used for blank check and for lookup.
     */
    private static String normalizeValue(String value) {
        return StringUtils.trimToNull(value);
    }

    private static <E extends Enum<E>> E handleBlankOrNull(Class<E> enumClass, Mode mode) {
        if (mode == Mode.LENIENT) {
            return null;
        }
        throw new IllegalArgumentException(
                "value must not be null or blank for type " + enumClass.getName());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Map<String, E> getOrBuildStableKeyIndex(Class<E> enumClass) {
        return (Map<String, E>) KEY_INDEX.computeIfAbsent(enumClass, EnumCompatParser::buildStableKeyIndex);
    }

    /**
     * Builds immutable stable-key → constant map. Constants with null key are
     * skipped (resolvable only by legacy name). Duplicate non-null keys
     * cause an immediate exception with enum class and conflicting constant names.
     */
    private static Map<String, ? extends Enum<?>> buildStableKeyIndex(Class<?> rawEnumClass) {
        @SuppressWarnings("unchecked")
        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) rawEnumClass;
        Map<String, Enum<?>> map = new HashMap<>();
        Enum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return Map.of();
        }
        for (Enum<?> constant : constants) {
            if (!(constant instanceof StableKeyedEnum keyed)) {
                continue;
            }
            String key = keyed.key();
            if (key == null) {
                // Intentionally skip: constant is only matchable by legacy name
                continue;
            }
            if (key.isBlank() || !key.equals(key.trim())) {
                throw new IllegalStateException(
                        "Stable key must be non-blank and trimmed in enum " + enumClass.getName()
                                + ": constant " + constant.name());
            }
            Enum<?> existing = map.put(key, constant);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate stable key in enum " + enumClass.getName()
                                + ": key='" + key + "'"
                                + " for constants " + existing.name() + " and " + constant.name());
            }
        }
        return Map.copyOf(map);
    }

    private static <E extends Enum<E>> IllegalArgumentException unknownValueException(
            String normalizedValue, Class<E> enumClass, Mode mode) {
        return new IllegalArgumentException(
                "Unknown enum value '" + normalizedValue + "' for type " + enumClass.getName()
                        + " (mode=" + mode + ")");
    }
}
