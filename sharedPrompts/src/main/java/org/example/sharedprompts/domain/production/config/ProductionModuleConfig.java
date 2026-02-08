package org.example.sharedprompts.domain.production.config;

import org.example.sharedprompts.domain.production.coordinator.ProductionRegistry;
import org.example.sharedprompts.domain.production.module.blog.BlogProductionModule;
import org.example.sharedprompts.domain.production.module.text.TextProductionModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductionModuleConfig {
    
    @Bean
    public ProductionRegistry productionRegistry(
            // TODO: EmailProductionModule, ImageProductionModule 추가 필요
            BlogProductionModule blogModule,
            TextProductionModule textModule
    ) {
        ProductionRegistry registry = new ProductionRegistry();
        // TODO: registry.register(emailModule);
        registry.register(blogModule);
        registry.register(textModule);
        // TODO: registry.register(imageModule);
        return registry;
    }
}

