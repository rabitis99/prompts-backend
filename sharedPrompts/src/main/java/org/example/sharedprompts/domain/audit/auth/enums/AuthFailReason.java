package org.example.sharedprompts.domain.audit.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증 실패 사유
 */
@Getter
@AllArgsConstructor
public enum AuthFailReason {

    USER_NOT_FOUND("사용자를 찾을 수 없음"),
    INVALID_PASSWORD("잘못된 비밀번호"),
    INVALID_REFRESH_TOKEN("잘못된 리프레시 토큰"),
    INVALID_ACCESS_TOKEN("잘못된 액세스 토큰"),
    OAUTH2_STATE_MISMATCH("OAuth2 상태 불일치"),
    OAUTH2_TOKEN_INVALID("OAuth2 토큰 유효하지 않음"),
    OAUTH2_INVALID_CODE("OAuth2 코드 유효하지 않음"),
    OAUTH2_AUTHENTICATION_FAILED("OAuth2 인증 실패");

    private final String displayName;
}

