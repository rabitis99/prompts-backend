package org.example.sharedprompts.auth.oauth.mapper.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getNestedMap;
import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getStringAttribute;
import static org.example.sharedprompts.auth.oauth.mapper.util.OAuth2AttributeUtils.getProviderId;

/**
 * Kakao OAuth2 사용자 정보 매퍼
 * 
 * Kakao 특수 케이스:
 * - id는 Long 타입으로 반환됨
 * - email은 kakao_account 하위에 있으며 null일 수 있음 (선택 동의)
 * - profile 정보는 kakao_account.profile 하위에 있음
 */
@Slf4j
@Component
public class KakaoOAuth2UserMapper extends AbstractOAuth2UserMapper {

    private static final String FIELD_PROVIDER_ID = "id";
    private static final String FIELD_KAKAO_ACCOUNT = "kakao_account";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PROFILE = "profile";
    private static final String FIELD_NICKNAME = "nickname";
    private static final String FIELD_IMAGE_URL = "profile_image_url";

    public KakaoOAuth2UserMapper() {
        super(Provider.KAKAO);
    }

    @Override
    protected OAuth2UserAttributes extractUserAttributes(Map<String, Object> attributes) {
        // Kakao는 id를 Long으로 반환
        String providerId = validateProviderId(getProviderId(attributes, FIELD_PROVIDER_ID));
        
        // kakao_account 하위에 email, profile 정보가 있음
        Map<String, Object> kakaoAccount = getNestedMap(attributes, FIELD_KAKAO_ACCOUNT);
        String email = null;
        String nickname = null;
        String imageUrl = null;
        
        if (kakaoAccount != null) {
            // Kakao email은 선택 동의이므로 null일 수 있음 (예외 처리하지 않음)
            email = getStringAttribute(kakaoAccount, FIELD_EMAIL);
            if (email == null) {
                log.debug("Kakao 사용자 email이 없습니다. (선택 동의 미완료)");
            }
            
            Map<String, Object> profile = getNestedMap(kakaoAccount, FIELD_PROFILE);
            if (profile != null) {
                nickname = getStringAttribute(profile, FIELD_NICKNAME);
                imageUrl = getStringAttribute(profile, FIELD_IMAGE_URL);
            }
        }
        
        return createBuilder()
                .providerId(providerId)
                .email(email)  // Kakao는 email이 null일 수 있음
                .nickname(nickname)
                .imageUrl(imageUrl)
                .build();
    }
}

