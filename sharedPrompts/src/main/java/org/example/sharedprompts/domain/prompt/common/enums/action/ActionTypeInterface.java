package org.example.sharedprompts.domain.prompt.common.enums.action;

import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/** ActionType 공통 계약. stable key, 표시명, ActionGroup만 보유. 출력/도메인 정책은 별도 registry. */
public interface ActionTypeInterface {

    /** 외부 계약용 안정 식별자 */
    String key();

    /** 한국어 표시명 */
    String getDisplayNameKo();

    /** 영어 표시명 */
    String getDisplayNameEn();

    /** 일본어 표시명 */
    String getDisplayNameJa();

    /** 상위 capability 그룹 */
    ActionGroup getActionGroup();
}