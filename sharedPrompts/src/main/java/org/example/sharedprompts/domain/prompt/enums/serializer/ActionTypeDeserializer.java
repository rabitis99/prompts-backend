package org.example.sharedprompts.domain.prompt.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.enums.ActionType;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.action.*;

import java.io.IOException;

public class ActionTypeDeserializer extends JsonDeserializer<ActionTypeInterface> {
    @Override
    public ActionTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ActionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return findInCategoryEnums(value);
        }
    }

    private ActionTypeInterface findInCategoryEnums(String value) {
        try {
            return ProductivityActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return DevelopmentActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CloudServicesActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return DevOpsActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CybersecurityActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CodingActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return ProgrammingActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return AiMlActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return AnalysisActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return MarketingActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return ContentActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CreativeActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return StudyActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return EducationActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return ResearchActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return BusinessActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return CustomerSupportActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return DesignActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return WritingActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return EtcActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return HealthFitnessActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        try {
            return SocialActionType.valueOf(value);
        } catch (IllegalArgumentException ignored) {}
        
        throw new IllegalArgumentException("Unknown ActionType: " + value);
    }
}

