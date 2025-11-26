package org.example.sharedprompts.global.jwt;

import lombok.Getter;
import org.example.sharedprompts.domain.user.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class PrincipalDetails implements UserDetails, OAuth2User {

    private final Long id;
    private final String email;
    private final String nickname;   // optional, null 허용
    private final Role role;

    private Map<String, Object> attributes; // OAuth2 용

    // 일반 로그인
    public PrincipalDetails(Long id, String email, String nickname, Role role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    // OAuth2 로그인용
    public PrincipalDetails(Long id, String email, String nickname, Role role, Map<String, Object> attributes) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.attributes = attributes;
    }

    // JWT Claims 에서 직접 생성
    public static PrincipalDetails fromJwtClaims(Long id, String email, String nickname, String role) {
        return new PrincipalDetails(
                id, email, nickname, Role.valueOf(role)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // password는 보관하지 않음
    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return nickname != null ? nickname : email; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
