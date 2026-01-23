package org.example.sharedprompts.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.jwt.model.PrincipalDetails;
import org.example.sharedprompts.auth.oauth.mapper.OAuth2UserMapper;
import org.example.sharedprompts.auth.oauth.mapper.factory.OAuth2UserMapperFactory;
import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.service.UserValidationService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * OAuth2 사용자 정보 로드 서비스
 * 
 * Spring Security의 OAuth2 인증 흐름에서 사용자 정보를 로드하는 Adapter 역할을 합니다.
 * 
 * 책임:
 * - OAuth2User 로드
 * - Provider 추출
 * - Attribute 매핑
 * - 사용자 조회/생성 위임
 * - 사용자 검증
 * - Principal 생성
 * 
 * 분리된 책임:
 * - 사용자 생성: UserRegistrationService
 * - 사용자 검증: UserValidationService
 * - 사용자 조회/생성 조율: OAuth2UserService
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserServiceImpl extends DefaultOAuth2UserService 
        implements CustomOAuth2UserService {

    private final OAuth2UserMapperFactory mapperFactory;
    private final OAuth2UserService oAuth2UserService;
    private final UserValidationService userValidationService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Provider 추출
        Provider provider = extractProvider(userRequest);
        
        // Provider별 Mapper를 사용하여 사용자 정보 추출
        OAuth2UserAttributes attributes = extractUserAttributes(oAuth2User, provider);

        // 사용자 조회 또는 생성
        User user = oAuth2UserService.findOrCreateUser(provider, attributes);

        // 사용자 검증 (차단, 삭제 등)
        userValidationService.validate(user);

        return createPrincipalDetails(user, oAuth2User.getAttributes());
    }

    /**
     * OAuth2UserRequest에서 Provider 추출
     * 
     * Factory.getMapper()에서 지원 여부를 검증하므로 여기서는 단순 변환만 수행합니다.
     */
    private Provider extractProvider(OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        try {
            return Provider.valueOf(registrationId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    ErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED,
                    String.format("지원하지 않는 OAuth2 제공자입니다: %s", registrationId)
            );
        }
    }

    /**
     * OAuth2User에서 사용자 속성 추출
     * Factory 패턴을 사용하여 동적으로 적절한 Mapper를 선택합니다.
     */
    private OAuth2UserAttributes extractUserAttributes(OAuth2User oAuth2User, Provider provider) {
        // Factory를 통해 Provider에 맞는 Mapper 조회
        OAuth2UserMapper mapper = mapperFactory.getMapper(provider);
        return mapper.map(oAuth2User);
    }

    private PrincipalDetails createPrincipalDetails(User user, Map<String, Object> attributes) {
        return new PrincipalDetails(
                user.getId(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getProviderId(),
                attributes
        );
    }
}

