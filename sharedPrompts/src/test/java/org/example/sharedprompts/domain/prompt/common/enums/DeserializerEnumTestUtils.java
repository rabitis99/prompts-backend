package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Shared test utility to obtain the canonical enum lists used by deserializers.
 * Use this as the single source of truth in coverage tests so that deserializer
 * and registry coverage stay in sync.
 */
public final class DeserializerEnumTestUtils {

    private DeserializerEnumTestUtils() {}

    @SuppressWarnings("unchecked")
    public static List<Class<? extends Enum<?>>> getActionTypeEnums() throws Exception {
        Field field = ActionTypeDeserializer.class.getDeclaredField("ACTION_TYPE_ENUMS");
        field.setAccessible(true);
        return (List<Class<? extends Enum<?>>>) field.get(null);
    }

    @SuppressWarnings("unchecked")
    public static List<Class<? extends Enum<?>>> getRoleTypeEnums() throws Exception {
        Field field = RoleTypeDeserializer.class.getDeclaredField("ROLE_TYPE_ENUMS");
        field.setAccessible(true);
        return (List<Class<? extends Enum<?>>>) field.get(null);
    }
}
