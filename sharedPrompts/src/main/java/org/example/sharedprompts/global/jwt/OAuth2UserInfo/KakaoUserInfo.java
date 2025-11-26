package org.example.sharedprompts.global.jwt.OAuth2UserInfo;

import java.util.Map;

public class KakaoUserInfo extends OAuth2UserInfo {

    public KakaoUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getNickname() {
        Map<String, Object> account = getMap(attributes, "kakao_account");
        Map<String, Object> profile = getMap(account, "profile");
        return profile != null ? (String) profile.get("nickname") : null;
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> account = getMap(attributes, "kakao_account");
        Map<String, Object> profile = getMap(account, "profile");
        return profile != null ? (String) profile.get("profile_image_url") : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = getMap(attributes, "kakao_account");
        Object email = account != null ? account.get("email") : null;
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
