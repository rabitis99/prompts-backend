package org.example.sharedprompts.module.domain.production.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Case-insensitive condition for storage type checking.
 * This allows both "S3" and "s3" (and any case variation) to match.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.context.annotation.Conditional(ConditionalOnStorageType.StorageTypeCondition.class)
public @interface ConditionalOnStorageType {
    
    String value();
    
    class StorageTypeCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            java.util.Map<String, Object> attributes = metadata.getAnnotationAttributes(
                    ConditionalOnStorageType.class.getName());
            
            if (attributes == null) {
                return false;
            }
            
            String expectedValue = (String) attributes.get("value");
            String actualValue = context.getEnvironment()
                    .getProperty("production.storage.type", "LOCAL");
            
            // Case-insensitive comparison
            boolean matches = expectedValue != null && 
                    expectedValue.trim().equalsIgnoreCase(actualValue != null ? actualValue.trim() : "");
            
            if (matches) {
                org.slf4j.LoggerFactory.getLogger(StorageTypeCondition.class)
                        .debug("Storage type condition matched: '{}' (expected: '{}')", 
                                actualValue, expectedValue);
            }
            
            return matches;
        }
    }
}

