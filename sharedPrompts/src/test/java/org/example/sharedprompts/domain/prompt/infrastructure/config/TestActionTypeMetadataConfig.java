package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.common.enums.action.metadata.ActionTypeMetadataProvider;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Optional;

/**
 * Test-only metadata provider: returns defaults for any key so Spring context loads
 * without requiring classpath action-type-*.properties. Production uses ClasspathActionTypeMetadataProvider.
 * Loaded before ActionTypeRegistryConfig so @ConditionalOnMissingBean skips classpath provider.
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestActionTypeMetadataConfig {

    @Bean
    public ActionTypeMetadataProvider actionTypeMetadataProvider() {
        return new ActionTypeMetadataProvider() {
            @Override
            public Optional<OutputBehaviorType> getOutputBehavior(String key) {
                return Optional.of(OutputBehaviorType.GENERAL_CONSULTATION);
            }

            @Override
            public Optional<TaskDomain> getTaskDomain(String key) {
                return Optional.of(TaskDomain.GENERAL);
            }
        };
    }
}
