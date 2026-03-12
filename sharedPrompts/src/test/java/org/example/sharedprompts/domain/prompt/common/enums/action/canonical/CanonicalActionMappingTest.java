package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures every ActionType maps to a canonical action; deserialization and key stability unchanged.
 */
@DisplayName("Canonical action mapping coverage and consistency")
class CanonicalActionMappingTest {

    private final CanonicalActionRegistry registry = new DefaultCanonicalActionRegistry();

    @Test
    @DisplayName("every ActionType constant maps to a non-null canonical action")
    void everyActionTypeMapsToCanonical() {
        List<Class<? extends Enum<?>>> enumClasses = ActionTypeCatalog.ACTION_ENUMS;
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) continue;
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) continue;
            for (Enum<?> constant : constants) {
                ActionTypeInterface action = (ActionTypeInterface) constant;
                var canonical = registry.toCanonical(action);
                assertThat(canonical)
                        .as("Every action must map to a canonical: " + enumClass.getSimpleName() + "." + constant.name() + " (key=" + action.key() + ")")
                        .isPresent();
                assertThat(canonical.get()).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("findByKey returns same canonical as toCanonical for every catalog key")
    void findByKeyMatchesToCanonical() {
        List<Class<? extends Enum<?>>> enumClasses = ActionTypeCatalog.ACTION_ENUMS;
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) continue;
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
    @DisplayName("sameCanonicalCapability is true for actions that map to same canonical")
    void sameCanonicalCapability() {
        // EDITING and PROOFREADING both map to TEXT_REVISION
        ActionTypeInterface a = EnumResolver.resolve("ACTION.WRITING.EDITING", ActionTypeCatalog.ACTION_ENUMS);
        ActionTypeInterface b = EnumResolver.resolve("ACTION.WRITING.PROOFREADING", ActionTypeCatalog.ACTION_ENUMS);
        assertThat(registry.sameCanonicalCapability(a, b)).isTrue();
        assertThat(registry.toCanonical(a)).contains(CanonicalActionId.TEXT_REVISION);
        assertThat(registry.toCanonical(b)).contains(CanonicalActionId.TEXT_REVISION);
    }

    @Test
    @DisplayName("no duplicate keys in catalog (sanity for registry build)")
    void noDuplicateKeys() {
        Set<String> keys = new HashSet<>();
        for (Class<? extends Enum<?>> enumClass : ActionTypeCatalog.ACTION_ENUMS) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) continue;
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                String key = ((ActionTypeInterface) constant).key();
                assertThat(keys.add(key)).as("Duplicate key: " + key).isTrue();
            }
        }
    }
}
