package org.example.sharedprompts.global.jwt;

import lombok.Getter;
import org.example.sharedprompts.domain.user.enums.Provider;
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
    private final String nickname;
    private final Role role;
    private final Map<String, Object> attributes;
    private final Provider provider;

    public PrincipalDetails(Long id, String email, String nickname, Role role) {
        this(id, email, nickname, role, null, null);
    }

    public PrincipalDetails(Long id, String email, String nickname, Role role,
                            Map<String, Object> attributes, Provider provider) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.attributes = attributes;
        this.provider = provider;
    }

    public static PrincipalDetails fromJwtClaims(Long id, String email, String nickname, String role, String provider) {
        return new PrincipalDetails(id, email, nickname, Role.valueOf(role), null, Provider.valueOf(provider));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return email; }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String getName() { return nickname != null ? nickname : email; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
