package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.BusinessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CareerActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CustomerSupportActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation.ContentCreationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation.EmailActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation.RecommendationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.creative.CreativeActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.design.DesignActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.AiMlActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CloudServicesActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CodingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CybersecurityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.DevOpsActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.DevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.ProgrammingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.education.EducationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.EtcActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.HealthFitnessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.LifestyleActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.SocialActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing.MarketingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.PersonalDevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ShoppingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.research.ResearchActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.BusinessRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.business.CustomerSupportRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.content_creation.ContentCreationRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.creative.CreativeRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.design.DesignRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.AiMlRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.development.DevelopmentRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.marketing.MarketingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.productivity.ProductivityRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.etc.SocialRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StableKeyedEnum 키 유일성 테스트")
class EnumKeyUniquenessTest {

    /** 모든 StableKeyedEnum 구현체를 나열하여 전역 키 유일성을 검증한다. */
    private static final List<Class<? extends Enum<?>>> STABLE_KEYED_ENUM_CLASSES = Arrays.asList(
            ToneType.class,
            StyleType.class,
            PromptCategory.class,
            ExperienceLevel.class,
            TaskDomain.class,
            ContentCreationRoleType.class,
            DesignRoleType.class,
            WritingRoleType.class,
            MarketingRoleType.class,
            DevelopmentRoleType.class,
            AiMlRoleType.class,
            CreativeRoleType.class,
            CustomerSupportRoleType.class,
            BusinessRoleType.class,
            SocialRoleType.class,
            ProductivityRoleType.class,
            WritingActionType.class,
            AnalysisActionType.class,
            CareerActionType.class,
            CybersecurityActionType.class,
            CreativeActionType.class,
            SocialActionType.class,
            MarketingActionType.class,
            PersonalDevelopmentActionType.class,
            EducationActionType.class,
            EtcActionType.class,
            CloudServicesActionType.class,
            BusinessActionType.class,
            ProgrammingActionType.class,
            HealthFitnessActionType.class,
            ShoppingActionType.class,
            ResearchActionType.class,
            DesignActionType.class,
            RecommendationActionType.class,
            EmailActionType.class,
            ProductivityActionType.class,
            DevelopmentActionType.class,
            LifestyleActionType.class,
            CodingActionType.class,
            CustomerSupportActionType.class,
            ContentCreationActionType.class,
            DevOpsActionType.class,
            AiMlActionType.class
    );

    @Test
    @DisplayName("모든 StableKeyedEnum의 key() 값은 전역에서 유일해야 한다")
    void stableKeysAreGloballyUnique() {
        Map<String, String> ownerByKey = new HashMap<>();
        for (Class<? extends Enum<?>> enumClass : STABLE_KEYED_ENUM_CLASSES) {
            assertUniqueKeysForEnum(enumClass, ownerByKey);
        }
    }

    private static void assertUniqueKeysForEnum(
            Class<? extends Enum<?>> enumClass,
            Map<String, String> ownerByKey
    ) {
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant instanceof StableKeyedEnum keyed) {
                String key = keyed.key();
                String existingOwner = ownerByKey.putIfAbsent(key, enumClass.getName() + "." + constant.name());
                assertThat(existingOwner)
                        .withFailMessage("Duplicate stable enum key '%s' for %s and %s", key, existingOwner,
                                enumClass.getName() + "." + constant.name())
                        .isNull();
            }
        }
    }
}

