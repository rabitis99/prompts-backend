package org.example.sharedprompts.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;

@Getter
@AllArgsConstructor
public class AuthUser {
    private final Long id;            // 사용자 고유 ID
    private final String email;       // 사용자 이메일 (OAuth2 로그인 시 필요)
    private final String nickname;    // 사용자 닉네임 (옵션, 필요 시 사용할 수 있음)
    private final Role role;          // 사용자 역할 (예: USER, ADMIN)
    private final Provider provider;  // 사용자 인증 제공자 (예: KAKAO, GOOGLE, etc.)
}
