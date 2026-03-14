package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;

/** 외부 입력 문자열을 ActionType으로 해석. (stable key → 레거시 순) */
public interface ActionTypeResolver {

    /** 문자열을 ActionType으로 해석. */
    ActionTypeInterface resolve(String value);
}