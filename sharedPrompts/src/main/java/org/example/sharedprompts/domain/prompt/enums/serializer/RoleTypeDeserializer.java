package org.example.sharedprompts.domain.prompt.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.enums.RoleType;
import org.example.sharedprompts.domain.prompt.enums.role.*;

import java.io.IOException;

public class RoleTypeDeserializer extends JsonDeserializer<RoleTypeInterface> {
    @Override
    public RoleTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RoleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return findInCategoryEnums(value);
        }
    }

    private RoleTypeInterface findInCategoryEnums(String value) {
        try {
            return ProductivityRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return DevelopmentRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return AiMlRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CybersecurityRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return MarketingRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return ContentRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CreativeRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return StudyRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return EducationRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return ResearchRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return BusinessRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CustomerSupportRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return DesignRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return WritingRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return EtcRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return HealthFitnessRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return SocialRoleType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        throw new IllegalArgumentException("Unknown RoleType: " + value);
    }
}

