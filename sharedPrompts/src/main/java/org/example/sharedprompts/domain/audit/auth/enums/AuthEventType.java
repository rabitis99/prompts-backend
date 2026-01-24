package org.example.sharedprompts.domain.audit.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증 이벤트 타입
 */
@Getter
@AllArgsConstructor
public enum AuthEventType {

    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAILED("로그인 실패"),
    LOGOUT("로그아웃"),
    TOKEN_REFRESH("토큰 갱신");

    private final String displayName;
}

