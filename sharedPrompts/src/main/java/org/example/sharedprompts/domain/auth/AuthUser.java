package org.example.sharedprompts.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;

@Getter
@AllArgsConstructor
public class AuthUser {

    /** 내부 사용자 고유 식별자 (DB PK) */
    private final Long id;

    /** 서비스 내에서 사용하는 표시용 닉네임 */
    private final String nickname;

    /** 사용자 권한 역할 (예: ROLE_USER, ROLE_ADMIN) */
    private final Role role;

    /** 인증에 사용된 OAuth2 제공자 (KAKAO, GOOGLE, NAVER 등) */
    private final Provider provider;

    /**
     * OAuth2 제공자에서 발급한 사용자 고유 ID
     * - provider + providerId 조합으로 사용자 식별
     * - nickname / 이메일 변경과 무관한 영구 식별자
     */
    private String providerId;
}
