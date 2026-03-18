package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeCompatibilityResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultOrderedActionTypeResolutionSource;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.OrderedActionTypeResolutionSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety test: every ActionType enum must implement ActionTypeInterface, have valid keys,
 * work with ActionTypeResolver (stable key + compatibility), and have no duplicate keys across enums.
 * Resolver is built without static holder (injectable).
 */
@DisplayName("ActionTypeDeserializer coverage and contract")
class ActionTypeDeserializerCoverageTest {

    private ActionTypeRegistry registry;
    private ActionTypeResolver resolver;

    @BeforeEach
    void setUp() {
        ActionTypeCatalog catalog = DeserializerEnumTestUtils.getActionTypeCatalog();
        OrderedActionTypeResolutionSource resolutionOrder = new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
        registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
        ActionTypeCompatibilityResolver compatibility = new ActionTypeCompatibilityResolver(resolutionOrder);
        resolver = new DefaultActionTypeResolver(registry, compatibility);
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
                ActionTypeInterface resolved = resolver.resolve(key)
                        .orElseThrow(() -> new AssertionError("Resolving key " + key + " for " + enumClass.getSimpleName() + " must not be empty"));
                assertThat(resolved)
                        .as("Key " + key + " should resolve back to " + enumClass.getSimpleName() + "." + constant.name())
                        .isSameAs(actionType);

                ActionTypeInterface resolvedByLegacyName = resolver.resolve(constant.name())
                        .orElseThrow(() -> new AssertionError("Resolving legacy name " + constant.name() + " for " + enumClass.getSimpleName() + " must not be empty"));
                assertThat(resolvedByLegacyName).isSameAs(actionType);

                String qualifiedLegacyName = enumClass.getSimpleName() + "." + constant.name();
                ActionTypeInterface resolvedByQualifiedLegacyName = resolver.resolve(qualifiedLegacyName)
                        .orElseThrow(() -> new AssertionError("Resolving legacy qualified name " + qualifiedLegacyName + " must not be empty"));
                assertThat(resolvedByQualifiedLegacyName).isSameAs(actionType);
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
