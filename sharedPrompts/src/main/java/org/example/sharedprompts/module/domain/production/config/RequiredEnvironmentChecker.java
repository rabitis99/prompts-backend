package org.example.sharedprompts.module.domain.production.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequiredEnvironmentChecker {
    
    private final Environment environment;
    
    public void checkRequiredVariables() {
        List<String> missing = new ArrayList<>();
        
        String[] required = {
            "production.storage.s3.bucket"
        };
        
        for (String key : required) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
            }
        }
        
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Required environment variables missing: " + String.join(", ", missing));
        }
    }
}

