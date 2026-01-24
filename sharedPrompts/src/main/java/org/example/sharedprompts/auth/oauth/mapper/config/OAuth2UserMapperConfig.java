package org.example.sharedprompts.auth.oauth.mapper.config;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.oauth.mapper.OAuth2UserMapper;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth2UserMapper 설정
 * 
 * Provider별 Mapper를 Map으로 등록합니다.
 */
@Configuration
@RequiredArgsConstructor
public class OAuth2UserMapperConfig {

    private final List<OAuth2UserMapper> mappers;

    /**
     * Provider별 Mapper Map 생성
     * 
     * 각 Mapper의 getProvider() 메서드를 사용하여 Map에 등록합니다.
     */
    @Bean
    public Map<Provider, OAuth2UserMapper> oAuth2UserMapperMap() {
        Map<Provider, OAuth2UserMapper> map = new HashMap<>();
        
        for (OAuth2UserMapper mapper : mappers) {
            Provider provider = mapper.getProvider();
            if (provider != null) {
                map.put(provider, mapper);
            }
        }
        
        return map;
    }
}

