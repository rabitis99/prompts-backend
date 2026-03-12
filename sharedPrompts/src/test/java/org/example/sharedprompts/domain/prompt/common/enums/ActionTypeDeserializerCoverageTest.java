package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
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
import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Safety test: every enum class in ActionTypeDeserializer.ACTION_TYPE_ENUMS must implement
 * ActionTypeInterface, have valid keys, work with EnumResolver, and have no duplicate keys
 * across enums (to avoid ambiguous resolution).
 */
@DisplayName("ActionTypeDeserializer coverage and contract")
class ActionTypeDeserializerCoverageTest {

    /**
     * Independent allowlist of all ActionType enum classes that must be registered in ActionTypeCatalog.
     * Keep in sync with {@link ActionTypeCatalog#ACTION_ENUMS}. If you add a new ActionType enum,
     * add it here and to the catalog; if you add only to one, this test fails.
     */
    private static final List<Class<? extends Enum<?>>> EXPECTED_ACTION_TYPE_ENUMS = List.of(
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
            ContentCreationActionType.class,
            CreativeActionType.class,
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

    @Test
    @DisplayName("ActionTypeCatalog contains exactly the expected ActionType enums (independent of deserializer)")
    void catalogMatchesExpectedActionTypeEnums() {
        assertThat(ActionTypeCatalog.ACTION_ENUMS)
                .as("ActionTypeCatalog.ACTION_ENUMS must match EXPECTED_ACTION_TYPE_ENUMS; add new enums to both")
                .containsExactlyInAnyOrderElementsOf(EXPECTED_ACTION_TYPE_ENUMS);
    }

    @Test
    @DisplayName("every enum in ActionTypeCatalog implements ActionTypeInterface and has valid key()")
    void everyEnumImplementsActionTypeInterfaceAndHasValidKey() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            assertThat(ActionTypeInterface.class.isAssignableFrom(enumClass))
                    .as("Enum " + enumClass.getName() + " must implement ActionTypeInterface")
                    .isTrue();

            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                assertThat(key).isNotBlank();
                assertThat(key.trim()).isEqualTo(key);
            }
        }
    }

    @Test
    @DisplayName("each constant key() round-trips via EnumResolver without throwing")
    void eachConstantKeyRoundTripsViaEnumResolver() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                ActionTypeInterface resolved = assertDoesNotThrow(
                        () -> EnumResolver.resolve(key, enumClasses),
                        "Resolving key " + key + " for " + enumClass.getSimpleName() + " must not throw"
                );
                assertThat(resolved)
                        .as("Key " + key + " should resolve back to " + enumClass.getSimpleName() + "." + constant.name())
                        .isSameAs(actionType);
            }
        }
    }

    @Test
    @DisplayName("no duplicate stable keys across ActionType enums (avoids ambiguous resolution)")
    void noDuplicateKeysAcrossActionTypeEnums() {
        List<Class<? extends Enum<?>>> enumClasses = DeserializerEnumTestUtils.getActionTypeEnums();
        Set<String> seenKeys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Class<? extends Enum<?>> enumClass : enumClasses) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) {
                continue;
            }
            for (Enum<?> constant : constants) {
                ActionTypeInterface actionType = (ActionTypeInterface) constant;
                String key = actionType.key();
                if (key != null && !key.isBlank()) {
                    if (!seenKeys.add(key)) {
                        duplicates.add(key + " (second in " + enumClass.getSimpleName() + "." + constant.name() + ")");
                    }
                }
            }
        }
        assertThat(duplicates)
                .as("Duplicate stable keys would make EnumResolver resolution order-dependent and ambiguous")
                .isEmpty();
    }
}
