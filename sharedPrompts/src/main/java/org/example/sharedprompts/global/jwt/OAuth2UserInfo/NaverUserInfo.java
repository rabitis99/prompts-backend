package org.example.sharedprompts.global.jwt.OAuth2UserInfo;

import java.util.Map;

public class NaverUserInfo extends OAuth2UserInfo {

    public NaverUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Map<String, Object> response = getMap(attributes, "response");
        return response != null ? (String) response.get("id") : null;
    }

    @Override
    public String getNickname() {
        Map<String, Object> response = getMap(attributes, "response");
        return response != null ? (String) response.get("nickname") : null;
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> response = getMap(attributes, "response");
        return response != null ? (String) response.get("profile_image") : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = getMap(attributes, "response");
        Object email = response != null ? response.get("email") : null;
        return email != null ? (String) email : "";
    }

    // 안전하게 Map을 가져오는 헬퍼
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}
