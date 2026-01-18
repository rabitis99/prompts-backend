package org.example.sharedprompts.global.aspect.util;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.global.annotation.AuditLogging;
import org.example.sharedprompts.global.jwt.PrincipalDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * Actor 정보를 추출하는 컴포넌트
 * 
 * 우선순위:
 * 1. Annotation 기반: adminIdParam 명시적 지정
 * 2. Annotation 기반: @CurrentUser AuthUser 파라미터
 * 3. SecurityContext에서 현재 사용자
 */
@Slf4j
@Component
public class ActorExtractor {

    /**
     * Actor ID 추출
     */
    public Long extractActorId(AuditLogging auditLogging, Object[] args, Parameter[] parameters) {
        try {
            // 1. Annotation 기반: adminIdParam이 명시적으로 지정된 경우 (가장 우선순위)
            if (!auditLogging.adminIdParam().isEmpty()) {
                Long adminId = ParameterExtractor.extractLongByName(
                        auditLogging.adminIdParam(), args, parameters);
                if (adminId != null) {
                    return adminId;
                }
                log.warn("adminIdParam으로 지정된 파라미터에서 값을 추출할 수 없음: paramName={}",
                        auditLogging.adminIdParam());
            }

            // 2. Annotation 기반: @CurrentUser AuthUser 파라미터에서 추출
            AuthUser authUser = extractAuthUserParameter(args, parameters);
            if (authUser != null && authUser.getId() != null) {
                return authUser.getId();
            }

            // 3. SecurityContext에서 현재 사용자 ID 추출
            return getActorIdFromSecurityContext();

        } catch (Exception e) {
            log.warn("액터 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Actor 식별자 추출 (nickname)
     */
    public String extractActorIdentifier(Object[] args, Parameter[] parameters) {
        try {
            // @CurrentUser AuthUser 파라미터에서 추출
            AuthUser authUser = extractAuthUserParameter(args, parameters);
            if (authUser != null && authUser.getNickname() != null) {
                return authUser.getNickname();
            }

            // SecurityContext에서는 identifier를 직접 추출할 수 없으므로 null 반환
            // Listener에서 actorId로 User를 조회할 때 identifier를 가져옴
            return null;

        } catch (Exception e) {
            log.warn("액터 식별자 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * @CurrentUser AuthUser 파라미터 추출
     */
    private AuthUser extractAuthUserParameter(Object[] args, Parameter[] parameters) {
        try {
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (param.isAnnotationPresent(CurrentUser.class) && args[i] instanceof AuthUser) {
                    return (AuthUser) args[i];
                }
            }
        } catch (Exception e) {
            log.warn("@CurrentUser AuthUser 파라미터 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * SecurityContext에서 현재 사용자 ID 추출
     */
    private Long getActorIdFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof PrincipalDetails principalDetails) {
                return principalDetails.getId();
            }
        } catch (Exception e) {
            log.warn("SecurityContext에서 사용자 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}

