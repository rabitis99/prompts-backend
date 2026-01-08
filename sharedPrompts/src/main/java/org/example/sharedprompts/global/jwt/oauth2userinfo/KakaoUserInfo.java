package org.example.sharedprompts.global.jwt.oauth2userinfo;

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
        return null;
    }
}
