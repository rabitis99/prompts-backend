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

    // ✅ OAuth2 / 일반 생성자
    public PrincipalDetails(
            Long id,
            String nickname,
            Role role,
            Provider provider,
            String providerId,
            Map<String, Object> attributes
    ) {
        super(id, nickname, role, provider, providerId);
        this.attributes = attributes;
    }

    // ✅ JWT claims 기반 생성자
    public static PrincipalDetails fromJwtClaims(
            Long id,
            String nickname,
            String role,
            String provider,
            String providerId
    ) {
        try {
            Role roleEnum = Role.valueOf(role);
            Provider providerEnum = Provider.valueOf(provider);

            return new PrincipalDetails(
                    id,
                    nickname,
                    roleEnum,
                    providerEnum,
                    providerId,
                    null
            );
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    String.format(
                            "유효하지 않은 role 또는 provider 값: role=%s, provider=%s",
                            role,
                            provider
                    )
            );
        }
    }

    // ================= UserDetails =================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(getRole().name()));
    }

    @Override
    public String getPassword() {
        return null; // OAuth2 / JWT 기반 인증
    }

    /**
     * username은 Security 내부에서 식별자로 사용됨
     * → email 제거 정책이므로 provider 기반 고유값 사용
     */
    @Override
    public String getUsername() {
        return getProvider().name() + "_" + getProviderId();
    }

    // ================= OAuth2User =================

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Map.of();
    }

    /**
     * OAuth2AuthorizedClient에서 사용하는 principalName
     * → 절대 null / empty 불가
     */
    @Override
    public String getName() {
        if (getProviderId() == null || getProviderId().isBlank()) {
            throw new IllegalStateException("providerId is empty for OAuth2 principal");
        }
        return getProvider().name() + "_" + getProviderId();
    }

    // ================= Account State =================

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
