package org.example.sharedprompts.auth.oauth.mapper.provider;

import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getNestedMapOrNull;
import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getStringAttribute;
import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getProviderId;

/**
 * Naver OAuth2 사용자 정보 매퍼
 * 
 * Naver 특수 케이스:
 * - 사용자 정보는 response 하위에 있음
 * - response가 null이면 예외 발생
 */
@Component
public class NaverOAuth2UserMapper extends AbstractOAuth2UserMapper {

    private static final String FIELD_RESPONSE = "response";
    private static final String FIELD_PROVIDER_ID = "id";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NICKNAME = "nickname";
    private static final String FIELD_IMAGE_URL = "profile_image";

    public NaverOAuth2UserMapper() {
        super(Provider.NAVER);
    }

    @Override
    protected OAuth2UserAttributes extractUserAttributes(Map<String, Object> attributes) {
        // Naver는 response 하위에 사용자 정보가 있음
        Map<String, Object> response = validateNestedMap(
                getNestedMapOrNull(attributes, FIELD_RESPONSE),
                FIELD_RESPONSE
        );
        
        String providerId = validateProviderId(getProviderId(response, FIELD_PROVIDER_ID));
        
        String email = getStringAttribute(response, FIELD_EMAIL);
        String nickname = getStringAttribute(response, FIELD_NICKNAME);
        String imageUrl = getStringAttribute(response, FIELD_IMAGE_URL);
        
        return createBuilder()
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .imageUrl(imageUrl)
                .build();
    }
}

