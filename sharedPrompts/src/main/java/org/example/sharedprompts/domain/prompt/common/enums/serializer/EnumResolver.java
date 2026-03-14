package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class EnumResolver {

    private EnumResolver() {
    }

    public static <T> T resolve(
            String value,
            Class<? extends T> baseEnum,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        validateValue(value);
        validateCategoryEnums(categoryEnums);

        if (baseEnum != null) {
            validateEnumClass(baseEnum, "baseEnum");

            Object parsed = tryParse(value, toEnumClass(baseEnum));
            if (parsed != null) {
                return castResult(parsed, value, baseEnum);
            }
        }

        return resolve(value, categoryEnums);
    }

    public static <T> T resolve(
            String value,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        validateValue(value);
        validateCategoryEnums(categoryEnums);

        for (Class<? extends Enum<?>> enumClass : categoryEnums) {
            Object parsed = tryParse(value, enumClass);
            if (parsed != null) {
                @SuppressWarnings("unchecked")
                T result = (T) parsed;
                return result;
            }
        }

        throw new IllegalArgumentException(buildUnknownValueMessage(value, categoryEnums));
    }

    private static void validateValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }
    }

    private static void validateCategoryEnums(List<Class<? extends Enum<?>>> categoryEnums) {
        Objects.requireNonNull(categoryEnums, "categoryEnums must not be null");

        Set<Class<? extends Enum<?>>> unique = new LinkedHashSet<>();
        for (int i = 0; i < categoryEnums.size(); i++) {
            Class<? extends Enum<?>> enumClass = categoryEnums.get(i);
            if (enumClass == null) {
                throw new IllegalArgumentException("categoryEnums must not contain null (index=" + i + ")");
            }
            validateEnumClass(enumClass, "categoryEnums[" + i + "]");
            if (!unique.add(enumClass)) {
                throw new IllegalArgumentException("Duplicate enum class in categoryEnums: " + enumClass.getName());
            }
        }
    }

    private static void validateEnumClass(Class<?> type, String paramName) {
        Objects.requireNonNull(type, paramName + " must not be null");
        if (!type.isEnum()) {
            throw new IllegalArgumentException(paramName + " must be an enum type: " + type.getName());
        }
    }

    private static String buildUnknownValueMessage(
            String value,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        String candidates = categoryEnums.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));

        return "Unknown enum value: '" + value + "'. "
                + "Tried candidate enums in order: [" + candidates + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T> T castResult(
            Object parsed,
            String value,
            Class<? extends T> expectedType
    ) {
        if (!expectedType.isInstance(parsed)) {
            throw new IllegalStateException(
                    "Resolved value '" + value + "' to type "
                            + parsed.getClass().getName()
                            + ", but expected assignable to "
                            + expectedType.getName()
            );
        }
        return (T) parsed;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> toEnumClass(Class<?> type) {
        return (Class<? extends Enum<?>>) type;
    }

    private static Object tryParse(String value, Class<? extends Enum<?>> enumClass) {
        return parseLenient(value, enumClass);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseLenient(String value, Class<? extends Enum<?>> enumClass) {
        return EnumCompatParser.parse(value, (Class) enumClass, EnumCompatParser.Mode.LENIENT);
    }
}