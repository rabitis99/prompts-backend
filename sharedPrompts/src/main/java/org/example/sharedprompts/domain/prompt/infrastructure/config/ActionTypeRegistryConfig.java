package org.example.sharedprompts.domain.prompt.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeCompatibilityResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultOrderedActionTypeResolutionSource;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.OrderedActionTypeResolutionSource;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.infrastructure.metadata.ClasspathActionTypeMetadataProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ActionType 인프라: catalog → registry → metadata provider → resolver/domain registries.
 * Bootstrap is explicit; no static metadata loader calls.
 */
@Configuration
public class ActionTypeRegistryConfig {

    @Bean
    public ActionTypeCatalog actionTypeCatalog() {
        return new DefaultActionTypeCatalog();
    }

    /** Compatibility 해석에만 사용하는 명시적 순서. Catalog 정의 집합과 분리. */
    @Bean
    public OrderedActionTypeResolutionSource orderedActionTypeResolutionSource(ActionTypeCatalog catalog) {
        return new DefaultOrderedActionTypeResolutionSource(catalog.getActionTypeEnumClasses());
    }

    @Bean
    public ActionTypeRegistry actionTypeRegistry(ActionTypeCatalog catalog) {
        return new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
    }

    @Bean
    @ConditionalOnMissingBean(ActionTypeMetadataProvider.class)
    public ActionTypeMetadataProvider actionTypeMetadataProvider() {
        return new ClasspathActionTypeMetadataProvider();
    }

    @Bean
    public ActionTypeCompatibilityResolver actionTypeCompatibilityResolver(OrderedActionTypeResolutionSource resolutionOrder) {
        return new ActionTypeCompatibilityResolver(resolutionOrder);
    }

    @Bean
    public ActionTypeResolver actionTypeResolver(
            ActionTypeRegistry actionTypeRegistry,
            ActionTypeCompatibilityResolver actionTypeCompatibilityResolver) {
        return new DefaultActionTypeResolver(actionTypeRegistry, actionTypeCompatibilityResolver);
    }

    @Bean
    public CanonicalActionRegistry canonicalActionRegistry(ActionTypeRegistry actionTypeRegistry) {
        return new DefaultCanonicalActionRegistry(actionTypeRegistry);
    }

    @Bean
    public ActionOutputBehaviorRegistry actionOutputBehaviorRegistry(
            ActionTypeRegistry actionTypeRegistry,
            ActionTypeMetadataProvider metadataProvider) {
        Map<String, OutputBehaviorType> keyToBehavior = new HashMap<>();
        for (ActionTypeInterface action : actionTypeRegistry.getAll()) {
            String key = action.key();
            keyToBehavior.put(key, metadataProvider.getOutputBehavior(key)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing output behavior metadata for key: " + key + " (fail-fast)")));
        }
        return new DefaultActionOutputBehaviorRegistry(keyToBehavior);
    }

    @Bean
    public ActionDomainRegistry actionDomainRegistry(
            ActionTypeRegistry actionTypeRegistry,
            ActionTypeMetadataProvider metadataProvider) {
        Map<String, TaskDomain> keyToDomain = new HashMap<>();
        for (ActionTypeInterface action : actionTypeRegistry.getAll()) {
            String key = action.key();
            keyToDomain.put(key, metadataProvider.getTaskDomain(key)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing task domain metadata for key: " + key + " (fail-fast)")));
        }
        return new DefaultActionDomainRegistry(keyToDomain);
    }
}