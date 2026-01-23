package org.example.sharedprompts.auth.security.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.oauth.CustomOAuth2UserService;
import org.example.sharedprompts.auth.oauth.handler.OAuth2FailureHandler;
import org.example.sharedprompts.auth.oauth.handler.OAuth2SuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

/**
 * OAuth2 로그인 설정 컴포넌트
 * 
 * <p>STATELESS 세션 정책과 호환되도록 쿠키 기반 AuthorizationRequestRepository를 사용합니다.
 * 기본 HttpSessionOAuth2AuthorizationRequestRepository는 STATELESS 모드에서 작동하지 않으므로
 * 쿠키 기반 저장소를 사용합니다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2Components {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    public void configureOAuth2Login(HttpSecurity http) throws Exception {
        http
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
                        )
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                );
    }
}

