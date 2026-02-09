package org.example.sharedprompts.domain.production.config;

import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.coordinator.ProductionRegistry;
import org.example.sharedprompts.domain.production.module.blog.BlogProductionModule;
import org.example.sharedprompts.domain.production.module.email.EmailProductionModule;
import org.example.sharedprompts.domain.production.module.text.TextProductionModule;
import org.example.sharedprompts.domain.production.module.image.ImageProductionModule;
import org.example.sharedprompts.domain.production.module.document.DocumentProductionModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class ProductionModuleConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ProductionModuleConfig.class);
    
    // Property name follows Spring Boot convention: production.active-modules
    // Supports both:
    // - ACTIVE_MODULES environment variable (backward compatibility, maps to active.modules)
    // - production.active-modules property (Spring Boot convention, kebab-case)
    // Spring Boot relaxed binding automatically maps PRODUCTION_ACTIVE_MODULES env var to production.active-modules
    @Value("${ACTIVE_MODULES:${production.active-modules:blog,email,text,image,document}}")
    private String activeModules;
    
    // TODO: 실제 외부 연동 시 리소스 낭비 방지를 위해 각 모듈에 @ConditionalOnProperty 추가 고려
    // 예: @ConditionalOnProperty(name = "modules.blog.enabled", havingValue = "true", matchIfMissing = false)
    // 현재는 모든 모듈이 Spring에 의해 인스턴스화되므로, ACTIVE_MODULES에 포함되지 않아도 빈이 생성됩니다.
    
    @Bean
    public ProductionRegistry productionRegistry(
            EmailProductionModule emailModule,
            BlogProductionModule blogModule,
            TextProductionModule textModule,
            ImageProductionModule imageModule,
            DocumentProductionModule documentModule
    ) {
        List<String> activeModuleList = Arrays.stream(activeModules.split(","))
                .map(String::trim)
                .toList();
        List<ProductionModule> modules = new ArrayList<>();
        
        Map<String, ProductionModule> moduleMap = Map.of(
            "blog", blogModule,
            "email", emailModule,
            "text", textModule,
            "image", imageModule,
            "document", documentModule
        );
        
        activeModuleList.forEach(name -> {
            ProductionModule module = moduleMap.get(name);
            if (module != null) {
                modules.add(module);
            } else {
                log.warn("Unknown module name in ACTIVE_MODULES: '{}'. Available modules: {}", 
                        name, moduleMap.keySet());
            }
        });
        
        return new ProductionRegistry(modules);
    }
}

