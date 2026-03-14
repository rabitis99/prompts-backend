package org.example.sharedprompts.domain.prompt.infrastructure.config;

import java.util.Map;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataLoader;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.DefaultActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeCompatibilityResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.DefaultActionTypeResolver;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.infrastructure.serialization.ActionTypeRegistryHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** ActionType 인프라: catalog 기준 registry·resolver·compatibility·metadata 구성. Jackson용 holder 초기화. */
@Configuration
public class ActionTypeRegistryConfig {

    @Bean
    public ActionTypeCatalog actionTypeCatalog() {
        return new DefaultActionTypeCatalog();
    }

    @Bean
    public ActionTypeRegistry actionTypeRegistry(ActionTypeCatalog catalog) {
        ActionTypeRegistry registry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
        ActionTypeRegistryHolder.setRegistry(registry);
        return registry;
    }

    @Bean
    public ActionTypeCompatibilityResolver actionTypeCompatibilityResolver(ActionTypeCatalog catalog) {
        return new ActionTypeCompatibilityResolver(catalog.getActionTypeEnumClasses());
    }

    @Bean
    public ActionTypeResolver actionTypeResolver(
            ActionTypeRegistry actionTypeRegistry,
            ActionTypeCompatibilityResolver actionTypeCompatibilityResolver) {
        ActionTypeResolver resolver =
                new DefaultActionTypeResolver(actionTypeRegistry, actionTypeCompatibilityResolver);
        ActionTypeRegistryHolder.setResolver(resolver);
        return resolver;
    }

    @Bean
    public ActionOutputBehaviorRegistry actionOutputBehaviorRegistry() {
        Map<String, OutputBehaviorType> keyToBehavior = ActionTypeMetadataLoader.loadKeyToOutputBehavior();
        return new DefaultActionOutputBehaviorRegistry(keyToBehavior);
    }

    @Bean
    public ActionDomainRegistry actionDomainRegistry() {
        Map<String, TaskDomain> keyToDomain = ActionTypeMetadataLoader.loadKeyToDomain();
        return new DefaultActionDomainRegistry(keyToDomain);
    }
}