package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.*;
import org.example.sharedprompts.domain.prompt.common.enums.role.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StableKeyedEnum 키 유일성 테스트")
class EnumKeyUniquenessTest {

    /** 모든 StableKeyedEnum 구현체를 나열하여 전역 키 유일성을 검증한다. */
    private static final List<Class<? extends Enum<?>>> STABLE_KEYED_ENUM_CLASSES = List.of(
            ToneType.class,
            StyleType.class,
            PromptCategory.class,
            ExperienceLevel.class,
            TaskDomain.class,
            ContentRoleType.class,
            DesignRoleType.class,
            WritingRoleType.class,
            MarketingRoleType.class,
            DevelopmentRoleType.class,
            AiMlRoleType.class,
            CreativeRoleType.class,
            CustomerSupportRoleType.class,
            StudyRoleType.class,
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
            StudyActionType.class,
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
            ContentActionType.class,
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

