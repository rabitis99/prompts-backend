package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import java.util.List;

public final class EnumResolver {
    private EnumResolver() {}
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T resolve(
            String value,
            Class<? extends T> baseEnum,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        if (baseEnum != null && Enum.class.isAssignableFrom(baseEnum)) {
            Object parsed = EnumCompatParser.parse(value, (Class<? extends Enum>) baseEnum, EnumCompatParser.Mode.STRICT);
            if (parsed != null) {
                return (T) parsed;
            }
        }

        for (Class<? extends Enum<?>> enumClass : categoryEnums) {
            Object parsed = EnumCompatParser.parse(value, (Class<? extends Enum>) enumClass, EnumCompatParser.Mode.LENIENT);
            if (parsed != null) {
                return (T) parsed;
            }
        }

        throw new IllegalArgumentException("Unknown enum value: " + value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T resolve(
            String value,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        for (Class<? extends Enum<?>> enumClass : categoryEnums) {
            Object parsed = EnumCompatParser.parse(value, (Class<? extends Enum>) enumClass, EnumCompatParser.Mode.LENIENT);
            if (parsed != null) {
                return (T) parsed;
            }
        }
        
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
}

