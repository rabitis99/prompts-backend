package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.core.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.BusinessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.CustomerSupportRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.content_creation.ContentCreationRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.creative.CreativeRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.design.DesignRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.AiMlRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.CybersecurityRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.DevelopmentRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.education.EducationRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.legal.LegalRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.EtcRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.HealthFitnessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.SocialRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.marketing.MarketingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.productivity.ProductivityRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.research.ResearchRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;

import java.io.IOException;
import java.util.List;

public class RoleTypeDeserializer extends JsonDeserializer<RoleTypeInterface> {
    
    private static final List<Class<? extends Enum<?>>> ROLE_TYPE_ENUMS = List.of(
            // 코어 역할
            CoreRoleType.class,

            // 도메인별 세부 역할 (주로 페르소나/설명용)
            ProductivityRoleType.class,
            DevelopmentRoleType.class,
            AiMlRoleType.class,
            CybersecurityRoleType.class,
            MarketingRoleType.class,
            ContentCreationRoleType.class,
            CreativeRoleType.class,
            EducationRoleType.class,
            LegalRoleType.class,
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
            // InputRequestDto에는 @NotNull 제약이 없으므로, null/blank 값에 대해 기본값 반환
            // PromptGuidelineBuilder에서 NPE 방지
            return EtcRoleType.GENERAL_CONSULTANT;
        }
        
        return EnumResolver.resolve(value, ROLE_TYPE_ENUMS);
    }
}

