package org.example.sharedprompts.domain.prompt.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.role.*;

import java.io.IOException;
import java.util.List;

public class RoleTypeDeserializer extends JsonDeserializer<RoleTypeInterface> {
    
    private static final List<Class<? extends Enum<?>>> ROLE_TYPE_ENUMS = List.of(
            ProductivityRoleType.class,
            DevelopmentRoleType.class,
            AiMlRoleType.class,
            CybersecurityRoleType.class,
            MarketingRoleType.class,
            ContentRoleType.class,
            CreativeRoleType.class,
            StudyRoleType.class,
            EducationRoleType.class,
            ResearchRoleType.class,
            BusinessRoleType.class,
            CustomerSupportRoleType.class,
            DesignRoleType.class,
            WritingRoleType.class,
            EtcRoleType.class,
            HealthFitnessRoleType.class,
            SocialRoleType.class
    );
    
    @Override
    public RoleTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        
        return EnumResolver.resolve(value, ROLE_TYPE_ENUMS);
    }
}

