package org.example.sharedprompts.global.jwt.oauth2userinfo;

import java.util.Map;

public class GoogleUserInfo extends OAuth2UserInfo {

    public GoogleUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Object id = attributes.get("sub");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getNickname() {
        Object name = attributes.get("name");
        return name != null ? name.toString() : null;
    }

    @Override
    public String getImageUrl() {
        Object picture = attributes.get("picture");
        return picture != null ? picture.toString() : null;
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? email.toString() : null;
    }
}
