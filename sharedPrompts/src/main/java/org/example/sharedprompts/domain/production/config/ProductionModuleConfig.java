package org.example.sharedprompts.domain.production.config;

import org.example.sharedprompts.domain.production.coordinator.ProductionRegistry;
import org.example.sharedprompts.domain.production.module.blog.BlogProductionModule;
import org.example.sharedprompts.domain.production.module.email.EmailProductionModule;
import org.example.sharedprompts.domain.production.module.text.TextProductionModule;
import org.example.sharedprompts.domain.production.module.image.ImageProductionModule;
import org.example.sharedprompts.domain.production.module.document.DocumentProductionModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class ProductionModuleConfig {
    
    @Value("${ACTIVE_MODULES:blog,email,text,image,document}")
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
        List<org.example.sharedprompts.domain.production.api.ProductionModule> modules = new ArrayList<>();
        
        Map<String, org.example.sharedprompts.domain.production.api.ProductionModule> moduleMap = Map.of(
            "blog", blogModule,
            "email", emailModule,
            "text", textModule,
            "image", imageModule,
            "document", documentModule
        );
        
        activeModuleList.forEach(name -> {
            org.example.sharedprompts.domain.production.api.ProductionModule module = moduleMap.get(name);
            if (module != null) {
                modules.add(module);
            }
        });
        
        return new ProductionRegistry(modules);
    }
}

