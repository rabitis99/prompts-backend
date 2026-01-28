package org.example.sharedprompts.global.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
/**
 * SecurityContext 접근 서비스
 * 
 * SecurityContext 접근을 중앙화하여 비즈니스 로직과 보안 책임을 분리합니다.
 * Facade나 Service 계층에서 SecurityContext를 직접 조작하지 않고,
 * 이 서비스를 통해 접근하도록 합니다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityContextService {

    /**
     * 현재 SecurityContext 조회
     *
     * <p>Spring Security 명세상 {@link SecurityContextHolder#getContext()} 는
     * 컨텍스트가 없어도 null이 아닌 빈 {@link SecurityContext} 를 반환합니다.
     *
     * @return SecurityContext (기본 전략에서는 항상 non-null)
     */
    public static SecurityContext getContext() {
        return SecurityContextHolder.getContext();
    }

    /**
     * 현재 인증 정보 조회
     * 
     * @return Authentication (없으면 null)
     */
    public static Authentication getAuthentication() {
        SecurityContext context = getContext();
        return context != null ? context.getAuthentication() : null;
    }
    /**
     * 현재 인증 여부 확인
     * 
     * @return 인증되어 있으면 true, 아니면 false
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * SecurityContext 디버그 정보 로깅
     * 
     * 디버그 모드에서만 실행되며, SecurityContext 상태를 로깅합니다.
     * 
     * @param context 추가 컨텍스트 정보 (예: 작업 이름)
     */
    public static void logSecurityContextDebug(String context) {
        if (log.isDebugEnabled()) {
            SecurityContext securityContext = getContext();
            boolean hasAuth = securityContext != null && securityContext.getAuthentication() != null;
            log.debug("[SecurityContext] {} - Thread: {}, hasAuth: {}", 
                    context,
                    Thread.currentThread().getName(),
                    hasAuth);
        }
    }
}

