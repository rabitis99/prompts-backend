package org.example.sharedprompts.global.aspect.util;

import org.example.sharedprompts.domain.audit.enums.AuditAction;

import java.lang.reflect.Parameter;

/**
 * 감사 로그 설명 템플릿을 처리하는 유틸리티 클래스
 * {paramName} 형식의 플레이스홀더를 파라미터 값으로 치환합니다.
 */
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
        for (int i = 0; i < parameters.length; i++) {
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
