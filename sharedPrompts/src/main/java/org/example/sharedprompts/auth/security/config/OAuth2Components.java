package org.example.sharedprompts.auth.security.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.oauth.CustomOAuth2UserService;
import org.example.sharedprompts.auth.oauth.handler.OAuth2FailureHandler;
import org.example.sharedprompts.auth.oauth.handler.OAuth2SuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class OAuth2Components {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    public void configureOAuth2Login(HttpSecurity http) throws Exception {
        http
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                );
    }
}

