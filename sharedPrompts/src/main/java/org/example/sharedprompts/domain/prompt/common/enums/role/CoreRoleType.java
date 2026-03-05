package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

/**
 * 엔진 레벨에서 의미 있는 핵심 역할 타입.
 *
 * <p>도메인별 세부 역할(enum)들은 대부분 페르소나 문구(UI) 용도인 반면,
 * 이 enum은 프롬프트 구조화/제약 인식 등 LLM 동작에 직접적인 영향을 주는
 * 코어 역할을 표현한다.</p>
 */
@Getter
@AllArgsConstructor
public enum CoreRoleType {

    PROMPT_ENGINEER,
    ANALYST,
    TECHNICAL_EXPERT,
    CREATIVE_DIRECTOR,
    EDUCATOR,
    EDITOR,
    QA_REVIEWER
}

