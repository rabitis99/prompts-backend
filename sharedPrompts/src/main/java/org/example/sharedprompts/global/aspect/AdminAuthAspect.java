package org.example.sharedprompts.global.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.jwt.PrincipalDetails;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @AdminOnly 어노테이션이 적용된 메서드에 대해 관리자 권한을 검증하는 AOP Aspect
 */
@Aspect
@Component
@Slf4j
@Order(0) // AuditLogAspect보다 먼저 실행되어 권한 검증 후 감사 로그 기록
public class AdminAuthAspect {

    @Before("@annotation(adminOnly) || @within(adminOnly)")
    public void checkAdminRole(JoinPoint joinPoint, AdminOnly adminOnly) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() 
                || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("관리자 권한 검증 실패: 인증되지 않은 사용자");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof PrincipalDetails principalDetails)) {
            log.warn("관리자 권한 검증 실패: PrincipalDetails 타입이 아님");
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Role userRole = principalDetails.getRole();

        if (userRole != Role.ROLE_ADMIN) {
            log.warn("관리자 권한 검증 실패: 사용자 ID={}, Role={}", principalDetails.getId(), userRole);
            throw new ApiException(ErrorCode.ADMIN_ONLY);
        }

        log.debug("관리자 권한 검증 성공: 사용자 ID={}", principalDetails.getId());
    }
}

