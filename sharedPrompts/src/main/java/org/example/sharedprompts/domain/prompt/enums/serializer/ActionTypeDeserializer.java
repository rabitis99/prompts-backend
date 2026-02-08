package org.example.sharedprompts.domain.prompt.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.action.*;

import java.io.IOException;
import java.util.List;

public class ActionTypeDeserializer extends JsonDeserializer<ActionTypeInterface> {
    
    private static final List<Class<? extends Enum<?>>> ACTION_TYPE_ENUMS = List.of(
            ProductivityActionType.class,
            DevelopmentActionType.class,
            CloudServicesActionType.class,
            DevOpsActionType.class,
            CybersecurityActionType.class,
            CodingActionType.class,
            ProgrammingActionType.class,
            AiMlActionType.class,
            AnalysisActionType.class,
            MarketingActionType.class,
            ContentActionType.class,
            CreativeActionType.class,
            StudyActionType.class,
            EducationActionType.class,
            ResearchActionType.class,
            BusinessActionType.class,
            CustomerSupportActionType.class,
            DesignActionType.class,
            WritingActionType.class,
            EtcActionType.class,
            HealthFitnessActionType.class,
            SocialActionType.class,
            CareerActionType.class,
            LifestyleActionType.class,
            PersonalDevelopmentActionType.class,
            RecommendationActionType.class,
            ShoppingActionType.class
    );
    
    @Override
    public ActionTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        
        return EnumResolver.resolve(value, ACTION_TYPE_ENUMS);
    }
}

