package org.example.sharedprompts.domain.prompt.common.enums.role.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;

/**
 * 엔진 레벨에서 의미 있는 핵심 역할 타입.
 *
 * <p>도메인별 세부 역할(enum)들은 대부분 페르소나 문구(UI) 용도인 반면,
 * 이 enum은 프롬프트 구조화/제약 인식 등 LLM 동작에 직접적인 영향을 주는
 * 코어 역할을 표현한다.</p>
 */
@Getter
@AllArgsConstructor
public enum CoreRoleType implements StableKeyedEnum {

    PROMPT_ENGINEER("CORE_ROLE.PROMPT_ENGINEER"),
    ANALYST("CORE_ROLE.ANALYST"),
    TECHNICAL_EXPERT("CORE_ROLE.TECHNICAL_EXPERT"),
    CREATIVE_DIRECTOR("CORE_ROLE.CREATIVE_DIRECTOR"),
    EDUCATOR("CORE_ROLE.EDUCATOR"),
    EDITOR("CORE_ROLE.EDITOR"),
    QA_REVIEWER("CORE_ROLE.QA_REVIEWER");

    private final String stableKey;

    @Override
    public String key() {
        return stableKey;
    }
}
