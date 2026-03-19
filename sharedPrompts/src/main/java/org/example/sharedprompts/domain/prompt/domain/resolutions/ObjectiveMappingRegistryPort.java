package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * Objective 해석 fallback: action 휴리스틱 + TaskDomain 기본값.
 *
 * <p>명시 매핑 이후 fallback으로만 사용.
 *
 * <p><b>내부 전략.</b> 구현: {@link ObjectiveMappingRegistry} (휴리스틱은 {@link ObjectiveHeuristicInferencePolicy}로 위임).
 */
public interface ObjectiveMappingRegistryPort {

    /**
     * 액션 타입 이름 기반 휴리스틱으로 목표를 추론한다.
     *
     * @param actionType 액션 타입 (null이면 empty)
     * @return 매칭되면 Optional에 담아 반환
     */
    Optional<PromptObjective> findByActionType(ActionTypeInterface actionType);

    /**
     * TaskDomain에 대한 도메인 기본 목표를 반환한다.
     *
     * @param taskDomain 작업 도메인 (null이면 REASONING 등 안전값)
     * @return 기본 목표, null 아님
     */
    PromptObjective getDomainDefault(TaskDomain taskDomain);
}
