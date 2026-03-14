package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

import java.util.Optional;

/** 외부 입력 문자열을 ActionType으로 해석. (stable key → 레거시 순) */
public interface ActionTypeResolver {

    /**
     * 문자열을 ActionType으로 해석.
     *
     * <p>실패 계약: 해석 불가 시 {@link Optional#empty()}를 반환한다. 구현체는 null을 반환하지 않으며,
     * 전제 조건 위반(예: null/blank 입력)에 대해서만 예외를 던질 수 있다.
     * 호출부는 {@code orElseThrow()}, {@code orElse(fallback)} 등으로 처리하면 된다.
     *
     * @param value 외부 입력 문자열 (stable key, 레거시 이름 등)
     * @return 해석된 ActionType을 담은 Optional, 미해결 시 empty (non-null)
     */
    Optional<ActionTypeInterface> resolve(String value);
}