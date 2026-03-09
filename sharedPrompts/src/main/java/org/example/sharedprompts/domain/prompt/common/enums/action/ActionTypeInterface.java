package org.example.sharedprompts.domain.prompt.common.enums.action;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;

import java.util.Optional;

/**
 * ActionType 공통 인터페이스
 * 모든 ActionType enum이 구현해야 하는 공통 메서드 정의.
 *
 * <p><b>책임 분리:</b> Enums identify; registries define compatibility and defaults.
 * Display names are UI metadata (consider i18n/descriptor for future). TaskDomain is stable classification;
 * default objective and output behavior are resolved via {@link ActionTypeBehaviorRegistry} and
 * {@link org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort}.</p>
 */
public interface ActionTypeInterface {
    /**
     * Stable identifier for serialization and equality.
     */
    String key();

    String getDisplayNameKo();
    String getDisplayNameEn();
    String getDisplayNameJa();

    /**
     * 이 ActionType이 속하는 TaskDomain을 반환한다.
     * <ul>
     *   <li>{@code Optional.of(X)} — 명시적으로 지정된 도메인</li>
     *   <li>{@code Optional.empty()} — 매핑 누락 (빌드 타임 테스트가 강제)</li>
     * </ul>
     */
    default Optional<TaskDomain> getTaskDomain() {
        return Optional.empty();
    }

    /**
     * 이 ActionType에 정책적으로 고정된 Objective가 있을 경우 반환한다.
     * <p><b>Active resolution must not use this.</b> {@link ObjectiveResolver} uses
     * {@link org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort} only.
     * Retained for compatibility; may be removed once all callers are removed.</p>
     *
     * @deprecated Policy belongs in registry. Use {@link org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort#findByActionType(ActionTypeInterface)} for resolution.
     */
    @Deprecated(since = "enum-cleanup", forRemoval = true)
    default PromptObjective getDefaultObjective() {
        return null;
    }

    /**
     * 이 ActionType이 궁극적으로 요구하는 출력 행동(OutputBehaviorType)을 반환한다.
     * <p>
     * 기본 구현은 {@link ActionTypeBehaviorRegistry}를 통해
     * enum 타입 기준의 거친 매핑을 수행한다.
     * </p>
     *
     * @return 코어 출력 행동 타입
     */
    default OutputBehaviorType getOutputBehavior() {
        return ActionTypeBehaviorRegistry.resolveBehavior(this);
    }

    /**
     * 언어 타입에 따라 적절한 표시 이름을 반환하는 디폴트 메서드
     */
    default String getDisplayNameByLang(LanguageType lang) {
        if (lang == null) {
            return getDisplayNameKo();
        }
        return switch (lang) {
            case KOREAN -> getDisplayNameKo();
            case ENGLISH -> getDisplayNameEn();
            case JAPANESE -> getDisplayNameJa();
        };
    }
}
