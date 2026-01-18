package org.example.sharedprompts.global.aspect.util;

import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.dto.admin.request.UserBlockRequestDto;

/**
 * AuditAction을 동적으로 결정하는 유틸리티 클래스
 */
public class AuditActionResolver {

    /**
     * 기본 Action과 afterState를 기반으로 최종 Action 결정
     * 
     * 예: action이 BLOCK이고 afterState가 UserBlockRequestDto인 경우,
     * getBlocked() 값을 확인하여 동적으로 BLOCK/UNBLOCK 결정
     */
    public static AuditAction resolve(AuditAction defaultAction, Object afterState) {
        // action이 BLOCK이고 afterState가 UserBlockRequestDto인 경우
        if (defaultAction == AuditAction.BLOCK && afterState instanceof UserBlockRequestDto requestDto) {
            Boolean blocked = requestDto.getBlocked();
            return Boolean.TRUE.equals(blocked) ? AuditAction.BLOCK : AuditAction.UNBLOCK;
        }

        return defaultAction;
    }
}
