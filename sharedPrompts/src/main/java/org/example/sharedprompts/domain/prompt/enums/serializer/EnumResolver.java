package org.example.sharedprompts.domain.prompt.enums.serializer;

import java.util.List;

public final class EnumResolver {
    private EnumResolver() {}
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T resolve(
            String value,
            Class<? extends T> baseEnum,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        if (baseEnum != null) {
            try {
                return (T) Enum.valueOf((Class) baseEnum, value);
            } catch (IllegalArgumentException ignored) {}
        }
        
        for (Class<? extends Enum<?>> enumClass : categoryEnums) {
            try {
                return (T) Enum.valueOf((Class) enumClass, value);
            } catch (IllegalArgumentException ignored) {}
        }
        
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T resolve(
            String value,
            List<Class<? extends Enum<?>>> categoryEnums
    ) {
        for (Class<? extends Enum<?>> enumClass : categoryEnums) {
            try {
                return (T) Enum.valueOf((Class) enumClass, value);
            } catch (IllegalArgumentException ignored) {}
        }
        
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
}

