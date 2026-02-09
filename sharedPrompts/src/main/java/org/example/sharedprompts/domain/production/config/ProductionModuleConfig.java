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

@Configuration
public class ProductionModuleConfig {
    
    @Value("${ACTIVE_MODULES:blog,email,text,image,document}")
    private String activeModules;
    
    @Bean
    public ProductionRegistry productionRegistry(
            EmailProductionModule emailModule,
            BlogProductionModule blogModule,
            TextProductionModule textModule,
            ImageProductionModule imageModule,
            DocumentProductionModule documentModule
    ) {
        List<String> activeModuleList = Arrays.asList(activeModules.split(","));
        List<org.example.sharedprompts.domain.production.api.ProductionModule> modules = new ArrayList<>();
        
        if (activeModuleList.contains("blog")) {
            modules.add(blogModule);
        }
        if (activeModuleList.contains("email")) {
            modules.add(emailModule);
        }
        if (activeModuleList.contains("text")) {
            modules.add(textModule);
        }
        if (activeModuleList.contains("image")) {
            modules.add(imageModule);
        }
        if (activeModuleList.contains("document")) {
            modules.add(documentModule);
        }
        
        return new ProductionRegistry(modules);
    }
}

