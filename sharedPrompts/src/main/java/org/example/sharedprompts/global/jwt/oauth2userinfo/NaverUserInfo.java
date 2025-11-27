package org.example.sharedprompts.global.jwt.oauth2userinfo;

import java.util.Map;

public class NaverUserInfo extends OAuth2UserInfo {

    public NaverUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Map<String, Object> response = getMap(attributes, "response");
        Object id = response != null ? response.get("id") : null;
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getNickname() {
        Map<String, Object> response = getMap(attributes, "response");
        Object nickname = response != null ? response.get("nickname") : null;
        return nickname != null ? String.valueOf(nickname) : null;
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> response = getMap(attributes, "response");
        Object imageUrl = response != null ? response.get("profile_image") : null;
        return imageUrl != null ? String.valueOf(imageUrl) : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = getMap(attributes, "response");
        Object email = response != null ? response.get("email") : null;
        return email != null ? String.valueOf(email) : null;
    }
}
