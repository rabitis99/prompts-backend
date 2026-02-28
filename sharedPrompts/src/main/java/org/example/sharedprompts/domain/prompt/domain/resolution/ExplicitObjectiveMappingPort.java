package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * ActionType → PromptObjective 명시 매핑 조회용 포트.
 *
 * <p>도메인 해석 체인에서 "명시 매핑" 단계만 추상화한다.
 * 등록(put)은 구현체·설정에서 수행하며, 도메인 서비스는 조회(get)에만 의존한다 (DIP).
 */
public interface ExplicitObjectiveMappingPort {

    /**
     * 주어진 액션 타입에 대한 명시적 목표가 있으면 반환한다.
     *
     * @param actionType 액션 타입 (null이면 empty)
     * @return 매핑된 목표가 있으면 Optional에 담아 반환
     */
    Optional<PromptObjective> get(ActionTypeInterface actionType);
}
