package org.example.sharedprompts.domain.prompt.enums.action;

import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ActionType의 getTaskDomain() 구현 검증 테스트
 * <p>모든 ActionType enum이 getTaskDomain()을 오버라이드했는지 검증</p>
 * <p>Optional.empty()를 반환하는 ActionType이 없어야 함 (빌드 타임 강제)</p>
 */
@DisplayName("ActionType getTaskDomain() 구현 검증 테스트")
class ActionTypeTaskDomainTest {

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

    @Test
    @DisplayName("모든 ActionType enum의 모든 값이 getTaskDomain()을 오버라이드했는지 검증")
    void allActionTypesOverrideGetTaskDomain() {
        List<String> unmappedActionTypes = new ArrayList<>();

        for (Class<? extends Enum<?>> enumClass : ACTION_TYPE_ENUMS) {
            Enum<?>[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants == null) {
                continue;
            }

            for (Enum<?> enumConstant : enumConstants) {
                if (!(enumConstant instanceof ActionTypeInterface actionType)) {
                    continue;
                }

                Optional<TaskDomain> taskDomain = actionType.getTaskDomain();
                if (taskDomain.isEmpty()) {
                    unmappedActionTypes.add(enumClass.getSimpleName() + "." + enumConstant.name());
                }
            }
        }

        assertThat(unmappedActionTypes)
                .as("getTaskDomain()이 Optional.empty()를 반환하는 ActionType이 있으면 안 됨. "
                        + "새 ActionType 추가 시 getTaskDomain()을 오버라이드해야 함.\n"
                        + "미매핑 ActionType: %s", unmappedActionTypes)
                .isEmpty();
    }

    @Test
    @DisplayName("모든 ActionType의 getTaskDomain()이 유효한 TaskDomain을 반환하는지 검증")
    void allActionTypesReturnValidTaskDomain() {
        for (Class<? extends Enum<?>> enumClass : ACTION_TYPE_ENUMS) {
            Enum<?>[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants == null) {
                continue;
            }

            for (Enum<?> enumConstant : enumConstants) {
                if (!(enumConstant instanceof ActionTypeInterface actionType)) {
                    continue;
                }

                Optional<TaskDomain> taskDomain = actionType.getTaskDomain();
                assertThat(taskDomain)
                        .as("%s.%s의 getTaskDomain()", enumClass.getSimpleName(), enumConstant.name())
                        .isPresent();

                TaskDomain domain = taskDomain.get();
                assertThat(domain)
                        .as("%s.%s의 TaskDomain", enumClass.getSimpleName(), enumConstant.name())
                        .isNotNull();
            }
        }
    }

    @Test
    @DisplayName("ActionTypeInterface의 기본 getTaskDomain()이 Optional.empty()를 반환하는지 검증")
    void defaultGetTaskDomainReturnsEmpty() {
        // ActionTypeInterface의 기본 구현이 Optional.empty()를 반환하는지 확인
        // (실제 enum 인스턴스가 아닌 인터페이스 자체의 기본 메서드 동작 확인)
        ActionTypeInterface defaultImpl = new ActionTypeInterface() {
            @Override
            public String getDisplayNameKo() {
                return "테스트";
            }

            @Override
            public String getDisplayNameEn() {
                return "Test";
            }

            @Override
            public String getDisplayNameJa() {
                return "テスト";
            }
        };

        Optional<TaskDomain> result = defaultImpl.getTaskDomain();
        assertThat(result)
                .as("기본 구현은 Optional.empty()를 반환해야 함")
                .isEmpty();
    }
}

