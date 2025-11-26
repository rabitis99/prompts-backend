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
}
