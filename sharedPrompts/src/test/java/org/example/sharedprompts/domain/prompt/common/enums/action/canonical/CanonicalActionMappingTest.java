package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeCompatibilityResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultActionTypeResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures every ActionType maps to an action group; deserialization and key stability unchanged.
 * Uses production catalog (DefaultActionTypeCatalog) as single source so taxonomy guardrails
 * validate the same enum list as runtime.
 */
@DisplayName("Action group mapping coverage and consistency")
class CanonicalActionMappingTest {

    private static final List<Class<? extends Enum<?>>> CATALOG_ENUMS = new DefaultActionTypeCatalog().getActionTypeEnumClasses();
    private static final ActionTypeRegistry ACTION_TYPE_REGISTRY = new ActionTypeRegistry(CATALOG_ENUMS);
    private static final ActionTypeResolver ACTION_TYPE_RESOLVER = new DefaultActionTypeResolver(
            ACTION_TYPE_REGISTRY,
            new ActionTypeCompatibilityResolver(CATALOG_ENUMS));
    private final CanonicalActionRegistry registry = new DefaultCanonicalActionRegistry(ACTION_TYPE_REGISTRY);

    @Test
    @DisplayName("every ActionType constant maps to a non-null action group")
    void everyActionTypeMapsToActionGroup() {
        for (Class<? extends Enum<?>> enumClass : CATALOG_ENUMS) {
            assertThat(ActionTypeInterface.class.isAssignableFrom(enumClass))
                    .as("ACTION_ENUMS must contain only ActionTypeInterface enums: " + enumClass.getName())
                    .isTrue();
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) continue;
            for (Enum<?> constant : constants) {
                ActionTypeInterface action = (ActionTypeInterface) constant;
                var actionGroup = registry.toCanonical(action);
                assertThat(actionGroup)
                        .as("Every action must map to an action group: " + enumClass.getSimpleName() + "." + constant.name() + " (key=" + action.key() + ")")
                        .isPresent();
                assertThat(actionGroup.get()).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("findByKey returns same action group as toCanonical for every catalog key")
    void findByKeyMatchesToCanonical() {
        for (Class<? extends Enum<?>> enumClass : CATALOG_ENUMS) {
            assertThat(ActionTypeInterface.class.isAssignableFrom(enumClass))
                    .as("ACTION_ENUMS must contain only ActionTypeInterface enums: " + enumClass.getName())
                    .isTrue();
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                ActionTypeInterface action = (ActionTypeInterface) constant;
                String key = action.key();
                var fromAction = registry.toCanonical(action);
                var fromKey = registry.findByKey(key);
                assertThat(fromKey).as("findByKey(" + key + ")").isEqualTo(fromAction);
            }
        }
    }

    @Test
    @DisplayName("sameCanonicalCapability is true for actions that map to same action group")
    void sameCanonicalCapability() {
        // EDITING and PROOFREADING both map to TEXT_REVISION
        ActionTypeInterface a = ACTION_TYPE_RESOLVER.resolve("ACTION.WRITING.EDITING");
        ActionTypeInterface b = ACTION_TYPE_RESOLVER.resolve("ACTION.WRITING.PROOFREADING");
        assertThat(registry.sameCanonicalCapability(a, b)).isTrue();
        assertThat(registry.toCanonical(a)).contains(ActionGroup.TEXT_REVISION);
        assertThat(registry.toCanonical(b)).contains(ActionGroup.TEXT_REVISION);
    }

    @Test
    @DisplayName("no duplicate keys in catalog (sanity for registry build)")
    void noDuplicateKeys() {
        Set<String> keys = new HashSet<>();
        for (Class<? extends Enum<?>> enumClass : CATALOG_ENUMS) {
            assertThat(ActionTypeInterface.class.isAssignableFrom(enumClass))
                    .as("ACTION_ENUMS must contain only ActionTypeInterface enums: " + enumClass.getName())
                    .isTrue();
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                String key = ((ActionTypeInterface) constant).key();
                assertThat(keys.add(key)).as("Duplicate key: " + key).isTrue();
            }
        }
    }
}
