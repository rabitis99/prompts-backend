package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

/** 외부 입력 문자열을 ActionType으로 해석. (stable key → 레거시 순) */
public interface ActionTypeResolver {

    /**
     * 문자열을 ActionType으로 해석.
     *
     * <p>실패 시 동작: 미해결/blank 입력에 대해 구현체는 반드시 다음 중 하나를 보장해야 한다.
     * <ul>
     *   <li>예외를 던지거나 (예: {@link IllegalArgumentException})</li>
     *   <li>null을 반환하거나</li>
     *   <li>계약된 fallback 값을 반환.</li>
     * </ul>
     * 호출부는 구현체 Javadoc/계약에 따라 null 체크 또는 예외 처리를 수행해야 한다.
     *
     * @param value 외부 입력 문자열 (stable key, 레거시 이름 등)
     * @return 해석된 ActionType, 또는 미해결 시 구현체 계약에 따른 null/fallback
     */
    ActionTypeInterface resolve(String value);
}