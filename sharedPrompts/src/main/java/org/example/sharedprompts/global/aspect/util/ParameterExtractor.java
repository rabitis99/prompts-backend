package org.example.sharedprompts.global.aspect.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Parameter;

/**
 * 메서드 파라미터에서 값을 추출하는 유틸리티 클래스
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParameterExtractor {

    /**
     * 파라미터 이름으로 Long 값 추출
     */
    public static Long extractLongByName(String paramName, Object[] args, Parameter[] parameters) {
        try {
            if (args == null || parameters == null) {
                return null;
            }
            if (parameters.length > 0 && !parameters[0].isNamePresent()) {
                log.warn("파라미터 이름이 유지되지 않아 이름 기반 추출을 사용할 수 없습니다. -parameters 옵션을 확인하세요.");
                return null;
            }
            int len = Math.min(args.length, parameters.length);
            for (int i = 0; i < len; i++) {
                if (parameters[i].getName().equals(paramName)) {
                    return toLong(args[i]);
                }
            }
        } catch (Exception e) {
            log.warn("파라미터 이름 기반 Long 추출 실패: paramName={}, error={}", paramName, e.getMessage());
        }
        return null;
    }

    /**
     * 첫 번째 Long 타입 파라미터 추출
     */
    public static Long extractFirstLong(Object[] args, Parameter[] parameters) {
        try {
            if (args == null || parameters == null) {
                return null;
            }
            int len = Math.min(args.length, parameters.length);
            for (int i = 0; i < len; i++) {
                Parameter param = parameters[i];
                Class<?> paramType = param.getType();
                if (paramType == Long.class || paramType == long.class) {
                    Long value = toLong(args[i]);
                    if (value != null) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("타입 기반 Long 파라미터 추출 실패: error={}", e.getMessage());
        }
        return null;
    }

    /**
     * 파라미터 이름으로 객체 값 추출
     */
    public static Object extractByName(String paramName, Object[] args, Parameter[] parameters) {
        try {
            if (args == null || parameters == null) {
                return null;
            }
            if (paramName == null || paramName.isEmpty()) {
                return null;
            }
            if (parameters.length > 0 && !parameters[0].isNamePresent()) {
                log.warn("파라미터 이름이 유지되지 않아 이름 기반 추출을 사용할 수 없습니다. -parameters 옵션을 확인하세요.");
                return null;
            }

            int len = Math.min(args.length, parameters.length);
            for (int i = 0; i < len; i++) {
                if (parameters[i].getName().equals(paramName)) {
                    return args[i];
                }
            }
        } catch (Exception e) {
            log.warn("파라미터 이름 기반 객체 추출 실패: paramName={}, error={}", paramName, e.getMessage());
        }
        return null;
    }

    /**
     * 파라미터 이름으로 특정 타입의 객체 추출
     */
    @SuppressWarnings("unchecked")
    public static <T> T extractByName(String paramName, Object[] args, Parameter[] parameters, Class<T> clazz) {
        try {
            if (clazz == null) {
                return null;
            }
            Object value = extractByName(paramName, args, parameters);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
        } catch (Exception e) {
            log.warn("파라미터 이름 기반 타입 추출 실패: paramName={}, type={}, error={}",
                    paramName, clazz != null ? clazz.getSimpleName() : "null", e.getMessage());
        }
        return null;
    }

    /**
     * Object를 Long으로 변환
     */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
