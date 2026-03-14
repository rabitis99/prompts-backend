package org.example.sharedprompts.domain.prompt.infrastructure.serialization;

import java.util.Objects;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;

/** Jackson 역직렬화용 브리지. deserializer가 registry/resolver 접근용. 도메인·앱 로직에서는 미사용. */
public final class ActionTypeRegistryHolder {

    private static volatile ActionTypeRegistry registry;
    private static volatile ActionTypeResolver resolver;

    private ActionTypeRegistryHolder() {
    }

    public static ActionTypeRegistry getRegistry() {
        ActionTypeRegistry current = registry;
        if (current == null) {
            throw new IllegalStateException(
                    "ActionTypeRegistry not initialized; ensure serialization config initializes it at startup");
        }
        return current;
    }

    public static ActionTypeResolver getResolver() {
        ActionTypeResolver current = resolver;
        if (current == null) {
            throw new IllegalStateException(
                    "ActionTypeResolver not initialized; ensure serialization config initializes it at startup");
        }
        return current;
    }

    public static synchronized void setRegistry(ActionTypeRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        if (ActionTypeRegistryHolder.registry != null) {
            throw new IllegalStateException("ActionTypeRegistry already initialized");
        }
        ActionTypeRegistryHolder.registry = registry;
    }

    public static synchronized void setResolver(ActionTypeResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        if (ActionTypeRegistryHolder.resolver != null) {
            throw new IllegalStateException("ActionTypeResolver already initialized");
        }
        ActionTypeRegistryHolder.resolver = resolver;
    }

    static synchronized void clearForTest() {
        registry = null;
        resolver = null;
    }
}