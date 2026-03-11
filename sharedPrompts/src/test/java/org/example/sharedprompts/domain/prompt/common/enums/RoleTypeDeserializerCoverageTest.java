package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every enum class in RoleTypeDeserializer.ROLE_TYPE_ENUMS must implement
 * RoleTypeInterface, have valid stable keys, work with EnumResolver, and have no duplicate
 * keys across enums (to avoid ambiguous resolution).
 */
@DisplayName("RoleTypeDeserializer coverage and contract")
class RoleTypeDeserializerCoverageTest {

    @Test
    @DisplayName("every enum in ROLE_TYPE_ENUMS implements RoleTypeInterface and has valid key()")
    void everyEnumImplementsRoleTypeInterfaceAndHasValidKey() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getRoleTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            assertThat(RoleTypeInterface.class.isAssignableFrom(enumClass))
                    .as("Enum " + enumClass.getName() + " must implement RoleTypeInterface")
                    .isTrue();

            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                RoleTypeInterface roleType = (RoleTypeInterface) constant;
                String key = roleType.key();
                assertThat(key).isNotBlank();
                assertThat(key.trim()).isEqualTo(key);
            }
        }
    }

    @Test
    @DisplayName("each constant key() round-trips via EnumResolver without throwing")
    void eachConstantKeyRoundTripsViaEnumResolver() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getRoleTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!RoleTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                RoleTypeInterface roleType = (RoleTypeInterface) constant;
                String key = roleType.key();
                assertDoesNotThrow(
                        () -> EnumResolver.resolve(key, enumClasses),
                        "Resolving key " + key + " for " + enumClass.getSimpleName() + " must not throw"
                );
            }
        }
    }

    @Test
    @DisplayName("no duplicate stable keys across RoleType enums (avoids ambiguous resolution)")
    void noDuplicateKeysAcrossRoleTypeEnums() throws Exception {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getRoleTypeEnums();
        Set<String> seenKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!RoleTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                RoleTypeInterface roleType = (RoleTypeInterface) constant;
                String key = roleType.key();
                if (key != null && !key.isBlank()) {
                    if (!seenKeys.add(key)) {
                        duplicates.add(key + " (second in " + enumClass.getSimpleName() + "." + constant.name() + ")");
                    }
                }
            }
        }
        assertThat(duplicates)
                .as("Duplicate stable keys would make EnumResolver resolution order-dependent and ambiguous")
                .isEmpty();
    }
}
