package org.example.sharedprompts.global.jwt.oauth2userinfo;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        // 방어적 복사 + 불변화
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public abstract String getId();

    public abstract String getNickname();

    public abstract String getImageUrl();

    public abstract String getEmail();

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMap(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}
