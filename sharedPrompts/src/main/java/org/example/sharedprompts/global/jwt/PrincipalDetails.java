package org.example.sharedprompts.global.jwt;

import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
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
            throw new IllegalArgumentException("유효하지 않은 role 또는 provider 값: " + role + ", " + provider, e);
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
        return attributes;
    }

    @Override
    public String getName() {
        return getNickname() != null ? getNickname() : getEmail();
    }

    // Account 상태 확인
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
