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

    /**
     * Initializes the holder atomically with both registry and resolver.
     * Must not be called when already initialized.
     */
    public static synchronized void initialize(ActionTypeRegistry registry, ActionTypeResolver resolver) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(resolver, "resolver");
        if (ActionTypeRegistryHolder.registry != null || ActionTypeRegistryHolder.resolver != null) {
            throw new IllegalStateException("ActionTypeRegistryHolder already initialized");
        }
        ActionTypeRegistryHolder.registry = registry;
        ActionTypeRegistryHolder.resolver = resolver;
    }

    /**
     * Clears static state so the holder can be re-initialized (e.g. context shutdown, second Spring context in same JVM).
     */
    public static synchronized void clear() {
        registry = null;
        resolver = null;
    }

    /**
     * Clears only if the holder still holds the given instances (safe for multiple ApplicationContexts in same JVM).
     * Use from context shutdown so only the context that owns the current registry/resolver clears.
     */
    public static synchronized void clearIfMatching(ActionTypeRegistry registry, ActionTypeResolver resolver) {
        if (ActionTypeRegistryHolder.registry == registry && ActionTypeRegistryHolder.resolver == resolver) {
            ActionTypeRegistryHolder.registry = null;
            ActionTypeRegistryHolder.resolver = null;
        }
    }
}