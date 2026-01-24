package org.example.sharedprompts.auth.oauth.mapper.provider;

import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getStringAttribute;
import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getProviderId;

/**
 * Google OAuth2 사용자 정보 매퍼
 * 
 * Google 특수 케이스:
 * - providerId는 "sub" 필드에 있음
 * - 모든 필드가 최상위 레벨에 있음
 */
@Component
public class GoogleOAuth2UserMapper extends AbstractOAuth2UserMapper {

    private static final String FIELD_PROVIDER_ID = "sub";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NICKNAME = "name";
    private static final String FIELD_IMAGE_URL = "picture";

    public GoogleOAuth2UserMapper() {
        super(Provider.GOOGLE);
    }

    @Override
    protected OAuth2UserAttributes extractUserAttributes(Map<String, Object> attributes) {
        String providerId = validateProviderId(getProviderId(attributes, FIELD_PROVIDER_ID));
        
        String email = getStringAttribute(attributes, FIELD_EMAIL);
        String nickname = getStringAttribute(attributes, FIELD_NICKNAME);
        String imageUrl = getStringAttribute(attributes, FIELD_IMAGE_URL);
        
        return createBuilder()
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .imageUrl(imageUrl)
                .build();
    }
}

