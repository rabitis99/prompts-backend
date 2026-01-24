package org.example.sharedprompts.auth.oauth.mapper.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.mapper.OAuth2UserMapper;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OAuth2 사용자 정보 매퍼 팩토리
 * 
 * Provider에 따라 적절한 Mapper를 동적으로 선택합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2UserMapperFactory {

    private final Map<Provider, OAuth2UserMapper> mapperMap;

    /**
     * Provider에 해당하는 Mapper 조회
     * 
     * @param provider Provider
     * @return OAuth2UserMapper
     * @throws ApiException 지원하지 않는 Provider인 경우
     */
    public OAuth2UserMapper getMapper(Provider provider) {
        if (provider == null) {
            throw new ApiException(
                    ErrorCode.OAUTH2_PROVIDER_REQUIRED,
                    "Provider는 null일 수 없습니다."
            );
        }

        OAuth2UserMapper mapper = mapperMap.get(provider);
        if (mapper == null) {
            log.error("지원하지 않는 OAuth2 Provider: {}", provider);
            throw new ApiException(
                    ErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED,
                    String.format("지원하지 않는 OAuth2 Provider입니다: %s", provider)
            );
        }

        log.debug("OAuth2 Mapper 선택: provider={}, mapper={}", provider, mapper.getClass().getSimpleName());
        return mapper;
    }

    /**
     * Provider가 지원되는지 확인
     * 
     * @param provider Provider
     * @return 지원 여부
     */
    public boolean isSupported(Provider provider) {
        return provider != null && mapperMap.containsKey(provider);
    }
}

