package org.example.sharedprompts.auth.jwt.model;

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

    // OAuth2 / 일반 생성자
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

    // JWT claims 기반 생성자 (최소 정보만 포함)
    // nickname, provider, providerId는 서비스 계층에서 DB 조회 필요
    public static PrincipalDetails fromJwtClaims(
            Long id,
            String role
    ) {
        try {
            Role roleEnum = Role.valueOf(role);

            // JWT에는 최소 정보만 포함되므로, 나머지는 임시값으로 설정
            // 실제 사용 시 서비스 계층에서 DB 조회 필요
            return new PrincipalDetails(
                    id,
                    null, // nickname은 DB에서 조회 필요
                    roleEnum,
                    null, // provider는 DB에서 조회 필요
                    null, // providerId는 DB에서 조회 필요
                    null
            );
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    String.format("유효하지 않은 role 값: role=%s", role)
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
     * JWT 기반 인증의 경우 userId를 사용
     */
    @Override
    public String getUsername() {
        // JWT 기반 인증의 경우 provider/providerId가 null일 수 있으므로 userId 사용
        if (getProvider() != null && getProviderId() != null && !getProviderId().isBlank()) {
            return getProvider().name() + "_" + getProviderId();
        }
        return String.valueOf(getId());
    }

    // ================= OAuth2User =================

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Map.of();
    }

    /**
     * OAuth2AuthorizedClient에서 사용하는 principalName
     * JWT 기반 인증의 경우 userId를 사용
     */
    @Override
    public String getName() {
        // JWT 기반 인증의 경우 provider/providerId가 null일 수 있으므로 userId 사용
        if (getProvider() != null && getProviderId() != null && !getProviderId().isBlank()) {
            return getProvider().name() + "_" + getProviderId();
        }
        return String.valueOf(getId());
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

