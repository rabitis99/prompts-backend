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
            EmailActionType.class,
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
            // InputRequestDto에는 @NotNull 제약이 없으므로, null/blank 값에 대해 기본값 반환
            // GuidelineBuilder에서 NPE 방지
            return EtcActionType.GENERAL_CONSULTATION;
        }
        
        return EnumResolver.resolve(value, ACTION_TYPE_ENUMS);
    }
}

