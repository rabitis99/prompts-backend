package org.example.sharedprompts.domain.audit.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;

/**
 * 인증 보안 이벤트
 * 
 * JPA Entity를 직접 포함하지 않고 스냅샷 값만 포함하여
 * AFTER_COMMIT 단계에서 안전하게 사용할 수 있도록 합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {

    /**
     * 이벤트 타입
     */
    private AuthEventType eventType;

    /**
     * 인증 제공자 (LOCAL, GOOGLE, KAKAO, NAVER 등)
     */
    private Provider provider;

    /**
     * providerId 또는 email의 해시값
     * 원문은 저장하지 않고 해시값만 저장합니다.
     */
    private String providerIdHash;

    /**
     * 사용자 ID (성공한 경우에만 존재)
     */
    private Long userId;

    /**
     * 실패 사유 (실패한 경우에만 존재)
     */
    private AuthFailReason failReason;

    /**
     * IP 주소
     */
    private String ipAddress;

    /**
     * User-Agent 정보
     */
    private String userAgent;
}

