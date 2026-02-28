package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActionType → PromptObjective 명시 매핑 (설정 시점에만 등록).
 *
 * <p>이름 기반 휴리스틱 없음. Spring 없음.
 * {@link ExplicitObjectiveMappingPort} 구현체.
 */
public class ExplicitObjectiveMapping implements ExplicitObjectiveMappingPort {

    private final Map<ActionTypeInterface, PromptObjective> map = new ConcurrentHashMap<>();

    public ExplicitObjectiveMapping() {
    }

    /** 명시적 (actionType, objective) 등록. Config 또는 테스트에서 호출. */
    public void put(ActionTypeInterface actionType, PromptObjective objective) {
        if (actionType != null && objective != null) {
            map.put(actionType, objective);
        }
    }

    @Override
    public Optional<PromptObjective> get(ActionTypeInterface actionType) {
        if (actionType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(actionType));
    }

    /** 명시 오버라이드·매핑이 없을 때만 사용. enum 이름 매칭 없음. */
    public static PromptObjective domainDefault(TaskDomain taskDomain) {
        if (taskDomain == null) {
            return PromptObjective.REASONING;
        }
        return switch (taskDomain) {
            case TECHNICAL -> PromptObjective.REASONING;
            case ANALYTICAL -> PromptObjective.FACTUAL;
            case CREATIVE -> PromptObjective.CREATIVE_WITH_CONSTRAINTS;
            case PRACTICAL -> PromptObjective.PLANNING;
            case EDUCATIONAL -> PromptObjective.REASONING;
            case GENERAL -> PromptObjective.REASONING;
        };
    }
}
