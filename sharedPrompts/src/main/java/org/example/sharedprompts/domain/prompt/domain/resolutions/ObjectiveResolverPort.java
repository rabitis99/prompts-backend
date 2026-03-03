package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

/**
 * PromptObjective 해석용 도메인 인터페이스.
 *
 * <p>(TaskDomain, ActionType) → PromptObjective. 도메인 서비스(예: PromptSpecFactory)는 이 추상에만 의존.
 *
 * <p><b>공개 API.</b> 구현: {@link ObjectiveResolver}.
 */
public interface ObjectiveResolverPort {

    /**
     * 주어진 도메인·액션에 대한 PromptObjective를 결정적으로 반환한다.
     *
     * @param taskDomain 이미 결정된 TaskDomain (null이면 GENERAL로 간주)
     * @param actionType 사용자 선택 액션 (null 가능)
     * @return 사용할 목표; null 아님
     */
    PromptObjective resolve(TaskDomain taskDomain, ActionTypeInterface actionType);
}
