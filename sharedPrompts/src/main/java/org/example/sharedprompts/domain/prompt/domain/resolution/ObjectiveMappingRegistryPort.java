package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

import java.util.Optional;

/**
 * Objective 해석 fallback: 액션 휴리스틱 + TaskDomain 기본값 조회 포트.
 *
 * <p>해석 체인에서 명시 매핑 이후 fallback으로만 사용.
 * 도메인 서비스는 이 추상에 의존하며, 구현(키워드 휴리스틱 등)은 인프라에서 주입한다 (DIP).
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
