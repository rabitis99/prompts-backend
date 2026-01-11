package org.example.sharedprompts.global.jwt;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.UserTerms;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.jwt.oauth2userinfo.*;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserServiceImpl extends DefaultOAuth2UserService implements CustomOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // provider 정보
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Provider provider;
        try {
            provider = Provider.valueOf(registrationId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST,
                    String.format("지원하지 않는 OAuth2 제공자입니다: %s", registrationId)
            );
        }
        OAuth2UserInfo userInfo = getOAuth2UserInfo(provider, attributes);

        // 사용자 조회 및 신규 생성
        User user = userRepository.findByProviderAndProviderId(provider, userInfo.getId())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(userInfo.getEmail())
                                .nickname(userInfo.getNickname() != null ? userInfo.getNickname() : RandomGenerator.randomNickname())
                                .role(Role.ROLE_USER)
                                .provider(provider)
                                .providerId(userInfo.getId())
                                .terms(UserTerms.ofDefault())
                                .signupCompleted(false)
                                .build()
                ));
        return new PrincipalDetails(
                user.getId(),
                user.getNickname(),
                user.getRole(),
                provider,
                user.getProviderId(),
                attributes
        );
    }

    private OAuth2UserInfo getOAuth2UserInfo(Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleUserInfo(attributes);
            case KAKAO -> new KakaoUserInfo(attributes);
            case NAVER -> new NaverUserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };
    }
}

