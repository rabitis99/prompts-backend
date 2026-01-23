package org.example.sharedprompts.auth.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.audit.auth.event.AuthEvent;
import org.example.sharedprompts.domain.audit.auth.util.AuthHashUtil;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 감사(Audit) 이벤트 발행 컴포넌트
 *
 * Application 레이어에서 사용할 수 있는 고수준 API를 제공하여
 * AuthServiceImpl 등에서 유스케이스 단위로 호출할 수 있게 합니다.
 *
 * 내부적으로는 (provider, providerId, userId, failReason) 시그니처 하나로 처리하되,
 * 공개 API는 유스케이스/의미 단위로 노출합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthAuditPublisher {

    private final ApplicationEventPublisher eventPublisher;

    // ===== 로그인 =====

    /** email(LOCAL) 기준 로그인 실패 */
    public void loginFailByEmail(Provider provider, String email, AuthFailReason failReason) {
        publish(AuthEventType.LOGIN_FAILED, provider, email, null, failReason);
    }

    /** providerId 기준 로그인 실패 (OAuth 등) */
    public void loginFailByProviderId(Provider provider, String providerId, AuthFailReason failReason) {
        publish(AuthEventType.LOGIN_FAILED, provider, providerId, null, failReason);
    }

    /** 어떤 principal도 모를 때의 로그인 실패 (예: OAuth2 코드 자체가 잘못된 경우) */
    public void loginFailWithoutPrincipal(AuthFailReason failReason) {
        publish(AuthEventType.LOGIN_FAILED, null, null, null, failReason);
    }

    /** User 기준 로그인 실패 (예: 비밀번호 불일치) */
    public void loginFailByUser(User user, AuthFailReason failReason) {
        if (user == null) {
            loginFailWithoutPrincipal(failReason);
            return;
        }
        publish(AuthEventType.LOGIN_FAILED, user.getProvider(), user.getProviderId(), user.getId(), failReason);
    }

    /** User 기준 로그인 성공 */
    public void loginSuccessByUser(User user) {
        if (user == null) {
            return;
        }
        loginSuccess(user.getProvider(), user.getProviderId(), user.getId());
    }

    /** provider, providerId, userId 직접 전달 로그인 성공 */
    public void loginSuccess(Provider provider, String providerId, Long userId) {
        publish(AuthEventType.LOGIN_SUCCESS, provider, providerId, userId, null);
    }

    // ===== 토큰 재발급 =====

    /** userId를 모르는 토큰 재발급 실패 */
    public void tokenRefreshFailWithoutUser(AuthFailReason failReason) {
        publish(AuthEventType.TOKEN_REFRESH, null, null, null, failReason);
    }

    /** userId 기준 토큰 재발급 실패 */
    public void tokenRefreshFailByUserId(Long userId, AuthFailReason failReason) {
        publish(AuthEventType.TOKEN_REFRESH, null, null, userId, failReason);
    }

    /** User 기준 토큰 재발급 성공 */
    public void tokenRefreshSuccessByUser(User user) {
        if (user == null) {
            return;
        }
        publish(AuthEventType.TOKEN_REFRESH, user.getProvider(), user.getProviderId(), user.getId(), null);
    }

    // ===== 로그아웃 =====

    /** userId 기준 로그아웃 실패 */
    public void logoutFailByUserId(Long userId, AuthFailReason failReason) {
        publish(AuthEventType.LOGOUT, null, null, userId, failReason);
    }

    /** User 기준 로그아웃 성공 */
    public void logoutSuccessByUser(User user) {
        if (user == null) {
            return;
        }
        publish(AuthEventType.LOGOUT, user.getProvider(), user.getProviderId(), user.getId(), null);
    }

    /**
     * 실제 이벤트 객체 생성 및 발행 공통 처리
     */
    private void publish(AuthEventType eventType,
                         Provider provider,
                         String providerId,
                         Long userId,
                         AuthFailReason failReason) {
        String ipAddress = HttpRequestUtils.getClientIpAddress();
        String userAgent = HttpRequestUtils.getUserAgent();

        String providerIdHash = providerId != null ? AuthHashUtil.hash(providerId) : null;

        try {
            AuthEvent event = AuthEvent.builder()
                    .eventType(eventType)
                    .provider(provider)
                    .providerIdHash(providerIdHash)
                    .userId(userId)
                    .failReason(failReason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("인증 이벤트 발행 실패: eventType={}, provider={}, userId={}",
                    eventType, provider, userId, e);
        }
    }
}

