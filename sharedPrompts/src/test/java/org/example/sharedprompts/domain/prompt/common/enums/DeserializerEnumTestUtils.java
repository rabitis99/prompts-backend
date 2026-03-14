package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Shared test utility to obtain the canonical enum lists used by deserializers.
 * ActionType list comes from {@link ActionTypeCatalog} (single source: DefaultActionTypeCatalog).
 * RoleType from RoleTypeDeserializer.
 */
public final class DeserializerEnumTestUtils {

    private static final String ROLE_TYPE_ENUMS_FIELD = "ROLE_TYPE_ENUMS";
    private static final ActionTypeCatalog ACTION_TYPE_CATALOG = new DefaultActionTypeCatalog();

    private DeserializerEnumTestUtils() {}

    public static List<Class<? extends Enum<?>>> getActionTypeEnums() {
        return ACTION_TYPE_CATALOG.getActionTypeEnumClasses();
    }

    public static Set<Class<? extends Enum<?>>> getActionTypeEnumsAsSet() {
        return Set.copyOf(ACTION_TYPE_CATALOG.getActionTypeEnumClasses());
    }

    public static ActionTypeCatalog getActionTypeCatalog() {
        return ACTION_TYPE_CATALOG;
    }

    @SuppressWarnings("unchecked")
    public static List<Class<? extends Enum<?>>> getRoleTypeEnums()
            throws NoSuchFieldException, IllegalAccessException {
        Field field = RoleTypeDeserializer.class.getDeclaredField(ROLE_TYPE_ENUMS_FIELD);
        field.setAccessible(true);
        return (List<Class<? extends Enum<?>>>) field.get(null);
    }
}
