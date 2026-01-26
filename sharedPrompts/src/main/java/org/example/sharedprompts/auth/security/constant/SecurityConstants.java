package org.example.sharedprompts.auth.security.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Spring Security 관련 상수
 * 
 * <p>SecurityContext 저장 및 복원에 사용되는 상수들을 정의합니다.
 * 모든 필드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

    /**
     * HttpServletRequest 속성에 SecurityContext를 저장할 때 사용하는 키
     * 
     * <p>비동기 요청 처리 시 SecurityContext를 유지하기 위해
     * HttpServletRequest 속성에 저장하는 데 사용됩니다.
     * 
     * <p>이 상수는 다음 클래스에서 공유됩니다:
     * <ul>
     *   <li>SecurityConfig.HttpServletRequestAttributeSecurityContextRepository</li>
     *   <li>WebAsyncSecurityConfig</li>
     *   <li>AsyncSecurityContextRestoreFilter</li>
     * </ul>
     */
    public static final String SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME = "SPRING_SECURITY_CONTEXT";
}

