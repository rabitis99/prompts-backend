package org.example.sharedprompts.global.aspect.util;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.audit.enums.AuditAction;

import java.lang.reflect.Parameter;

/**
 * 감사 로그 설명 템플릿을 처리하는 유틸리티 클래스
 * {paramName} 형식의 플레이스홀더를 파라미터 값으로 치환합니다.
 */
@Slf4j
public class DescriptionBuilder {

    /**
     * 설명 템플릿에서 플레이스홀더를 치환하여 최종 설명 생성
     */
    public static String build(String descriptionTemplate, AuditAction action,
                               Object[] args, Parameter[] parameters, Long entityId) {
        if (descriptionTemplate == null || descriptionTemplate.isEmpty()) {
            return action.getDisplayName();
        }

        String description = descriptionTemplate;

        // {entityId} 치환
        if (entityId != null) {
            description = description.replace("{entityId}", String.valueOf(entityId));
        }

        // 파라미터 값 치환
        if (args == null || parameters == null) {
            return description;
        }

        if (parameters.length > 0 && !parameters[0].isNamePresent()) {
            log.warn("파라미터 이름이 유지되지 않아 템플릿 치환을 사용할 수 없습니다. -parameters 옵션을 확인하세요.");
            return description; // 치환하지 않고 원본 반환
        }

        int len = Math.min(args.length, parameters.length);
        for (int i = 0; i < len; i++) {
            String paramName = parameters[i].getName();
            String placeholder = "{" + paramName + "}";
            if (description.contains(placeholder)) {
                Object value = args[i];
                String valueStr = value != null ? value.toString() : "null";
                description = description.replace(placeholder, valueStr);
            }
        }

        return description;
    }
}
