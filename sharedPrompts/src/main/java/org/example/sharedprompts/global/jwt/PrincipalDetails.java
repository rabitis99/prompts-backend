package org.example.sharedprompts.global.jwt;

import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrincipalDetails extends AuthUser implements UserDetails, OAuth2User {

    private final Map<String, Object> attributes;

    // Constructor
    public PrincipalDetails(Long id, String email, String nickname, Role role, Provider provider, Map<String, Object> attributes) {
        super(id, email, nickname, role, provider);
        this.attributes = attributes;
    }

    // JWT claims에서 PrincipalDetails 생성
    public static PrincipalDetails fromJwtClaims(Long id, String email, String nickname, String role, String provider) {
        try {
            Role roleEnum = Role.valueOf(role);
            Provider providerEnum = Provider.valueOf(provider);
            return new PrincipalDetails(id, email, nickname, roleEnum, providerEnum, null);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST ,"유효하지 않은 role 또는 provider 값: ");
        }
    }

    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(getRole().name()));
    }

    @Override
    public String getPassword() {
        return null; // JWT나 OAuth2 방식에서는 비밀번호가 없을 수 있음
    }

    @Override
    public String getUsername() {
        return getEmail(); // 이메일을 사용자 이름으로 사용
    }

    // OAuth2User 구현
    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Map.of();
    }

    @Override
    public String getName() {
        return getNickname() != null ? getNickname() : getEmail();
    }

    // TODO: 계정 상태 플래그 연동
    //  - 현재는 모든 계정 상태(isAccountNonExpired, isAccountNonLocked,
    //  - isCredentialsNonExpired, isEnabled)를 true로 고정하여 사용합니다.
    //  - 추후 User 도메인에 enabled, locked, deletedAt, credentialsExpiredAt 등의
    //  - 필드가 도입되면 아래 메서드들이 해당 상태를 반영하도록 수정할 예정입니다.

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
