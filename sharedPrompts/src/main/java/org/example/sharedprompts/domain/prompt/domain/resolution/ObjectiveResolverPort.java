package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

/**
 * Port: (TaskDomain, ActionType) → PromptObjective.
 *
 * <p>해석 책임은 이 추상화에만 두며, 애플리케이션/팩토리는 구현에 의존하지 않는다 (DIP).
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
