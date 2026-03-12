package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Shared test utility to obtain the canonical enum lists used by deserializers.
 * ActionType list comes from {@link ActionTypeCatalog}; RoleType from RoleTypeDeserializer.
 */
public final class DeserializerEnumTestUtils {

    private static final String ROLE_TYPE_ENUMS_FIELD = "ROLE_TYPE_ENUMS";

    private DeserializerEnumTestUtils() {}

    public static List<Class<? extends Enum<?>>> getActionTypeEnums() {
        return ActionTypeCatalog.ACTION_ENUMS;
    }

    @SuppressWarnings("unchecked")
    public static List<Class<? extends Enum<?>>> getRoleTypeEnums()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = RoleTypeDeserializer.class.getDeclaredField(ROLE_TYPE_ENUMS_FIELD);
        field.setAccessible(true);
        return (List<Class<? extends Enum<?>>>) field.get(null);
    }
}
