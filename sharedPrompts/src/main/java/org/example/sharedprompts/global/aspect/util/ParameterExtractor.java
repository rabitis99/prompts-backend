package org.example.sharedprompts.global.aspect.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Parameter;

/**
 * 메서드 파라미터에서 값을 추출하는 유틸리티 클래스
 */
@Slf4j
public class ParameterExtractor {

    /**
     * 파라미터 이름으로 Long 값 추출
     */
    public static Long extractLongByName(String paramName, Object[] args, Parameter[] parameters) {
        try {
            for (int i = 0; i < parameters.length; i++) {
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
            for (int i = 0; i < parameters.length; i++) {
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
            if (paramName == null || paramName.isEmpty()) {
                return null;
            }

            for (int i = 0; i < parameters.length; i++) {
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
            Object value = extractByName(paramName, args, parameters);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
        } catch (Exception e) {
            log.warn("파라미터 이름 기반 타입 추출 실패: paramName={}, type={}, error={}",
                    paramName, clazz.getSimpleName(), e.getMessage());
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
