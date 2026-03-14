package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeCompatibilityResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultActionTypeResolver;
import org.example.sharedprompts.domain.prompt.infrastructure.serialization.ActionTypeRegistryHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every ActionType enum must implement ActionTypeInterface, have valid keys,
 * work with ActionTypeResolver (stable key + compatibility), and have no duplicate keys across enums.
 */
@DisplayName("ActionTypeDeserializer coverage and contract")
class ActionTypeDeserializerCoverageTest {

    private ActionTypeRegistry registry;
    private ActionTypeResolver resolver;

    @BeforeEach
    void setUp() {
        org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog catalog = DeserializerEnumTestUtils.getActionTypeCatalog();
        registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
        ActionTypeCompatibilityResolver compatibility = new ActionTypeCompatibilityResolver(catalog.getActionTypeEnumClasses());
        resolver = new DefaultActionTypeResolver(registry, compatibility);
        ActionTypeRegistryHolder.setRegistry(registry);
        ActionTypeRegistryHolder.setResolver(resolver);
    }

    @AfterEach
    void tearDown() {
        ActionTypeRegistryHolder.setRegistry(null);
        ActionTypeRegistryHolder.setResolver(null);
    }

    @Test
    @DisplayName("registry size equals total ActionType constants from catalog (single source)")
    void registrySizeMatchesCatalog() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        int totalConstants = 0;
        for (Class<? extends Enum<?>> c : enumClasses) {
            if (ActionTypeInterface.class.isAssignableFrom(c)) {
                Enum<?>[] constants = c.getEnumConstants();
                if (constants != null) totalConstants += constants.length;
            }
        }
        assertThat(registry.getAll())
                .as("Registry must contain every action from catalog")
                .hasSize(totalConstants);
    }

    @Test
    @DisplayName("every enum implements ActionTypeInterface and has valid key()")
    void everyEnumImplementsActionTypeInterfaceAndHasValidKey() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            assertThat(ActionTypeInterface.class.isAssignableFrom(enumClass))
                    .as("Enum " + enumClass.getName() + " must implement ActionTypeInterface")
                    .isTrue();

            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                assertThat(key).isNotBlank();
                assertThat(key.trim()).isEqualTo(key);
            }
        }
    }

    @Test
    @DisplayName("each constant key() round-trips via ActionTypeResolver.resolve without throwing")
    void eachConstantKeyRoundTripsViaRegistry() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                ActionTypeInterface resolved = assertDoesNotThrow(
                        () -> resolver.resolve(key),
                        "Resolving key " + key + " for " + enumClass.getSimpleName() + " must not throw"
                );
                assertThat(resolved)
                        .as("Key " + key + " should resolve back to " + enumClass.getSimpleName() + "." + constant.name())
                        .isSameAs(actionType);
            }
        }
    }

    @Test
    @DisplayName("no duplicate stable keys across ActionType enums (avoids ambiguous resolution)")
    void noDuplicateKeysAcrossActionTypeEnums() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        Set<String> seenKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
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
