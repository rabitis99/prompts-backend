package org.example.sharedprompts.auth.oauth.mapper.util;

import java.util.Map;

/**
 * OAuth2 속성 추출 유틸리티
 * 
 * Provider별 Mapper에서 공통으로 사용하는 유틸리티 메서드
 */
public final class OAuth2AttributeUtils {

    private OAuth2AttributeUtils() {
        // 유틸리티 클래스는 인스턴스화 불가
    }

    /**
     * Map에서 String 속성 추출
     * 
     * @param attributes 속성 Map
     * @param key 키
     * @return String 값 또는 null
     */
    public static String getStringAttribute(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Map에서 중첩된 Map 추출
     * 
     * @param map 상위 Map
     * @param key 키
     * @return 중첩된 Map 또는 null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    /**
     * Map에서 중첩된 Map 추출 및 검증
     * 
     * @param map 상위 Map
     * @param key 키
     * @return 중첩된 Map (null이거나 비어있으면 null)
     */
    public static Map<String, Object> getNestedMapOrNull(Map<String, Object> map, String key) {
        Map<String, Object> nested = getNestedMap(map, key);
        return (nested != null && !nested.isEmpty()) ? nested : null;
    }

    /**
     * Map에서 Provider ID 추출 (Object를 String으로 변환)
     * 
     * @param attributes 속성 Map
     * @param key 키 (예: "id", "sub")
     * @return Provider ID 문자열 또는 null
     */
    public static String getProviderId(Map<String, Object> attributes, String key) {
        if (attributes == null) {
            return null;
        }
        Object idObj = attributes.get(key);
        return idObj != null ? String.valueOf(idObj) : null;
    }
}

