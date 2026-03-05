package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    /**
     * Per-enum-class index from stable key to enum constant.
     * Built lazily and treated as immutable snapshots for each class.
     */
    private static final ConcurrentMap<Class<?>, Map<String, ? extends Enum<?>>> KEY_INDEX =
            new ConcurrentHashMap<>();

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
            @SuppressWarnings("unchecked")
            Map<String, E> index = (Map<String, E>) KEY_INDEX.computeIfAbsent(enumClass, cls -> {
                Map<String, E> map = new HashMap<>();
                for (E constant : enumClass.getEnumConstants()) {
                    StableKeyedEnum keyed = (StableKeyedEnum) constant;
                    String key = keyed.key();
                    if (key != null) {
                        map.putIfAbsent(key, constant);
                    }
                }
                return Map.copyOf(map);
            });

            E matched = index.get(value);
            if (matched != null) {
                return matched;
            }
        }

        if (mode == Mode.LENIENT) {
            return null;
        }
        throw new IllegalArgumentException(
                "Unknown enum value '" + value + "' for type " + enumClass.getName());
    }
}

