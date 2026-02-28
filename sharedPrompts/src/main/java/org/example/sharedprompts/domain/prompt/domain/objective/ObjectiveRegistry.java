package org.example.sharedprompts.domain.prompt.domain.objective;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;

/**
 * Objective → ObjectiveProfile 조회 계약.
 * 구현체({@link DefaultObjectiveRegistry})는 순수 Java이며, Spring 레이어에서 @Bean으로 등록한다.
 */
public interface ObjectiveRegistry {

    /**
     * @throws IllegalArgumentException Objective에 대응하는 프로파일이 없을 때
     */
    ObjectiveProfile get(PromptObjective objective);
}
