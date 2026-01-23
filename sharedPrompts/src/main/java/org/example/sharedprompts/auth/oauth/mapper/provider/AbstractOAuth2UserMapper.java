package org.example.sharedprompts.auth.oauth.mapper.provider;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.mapper.OAuth2UserMapper;
import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.auth.oauth.mapper.provider.exception.OAuth2MappingException;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

/**
 * OAuth2 사용자 정보 매퍼 추상 클래스
 * 
 * Provider별 Mapper의 공통 로직을 제공합니다.
 * Template Method 패턴을 사용하여 공통 흐름을 정의하고,
 * 각 구현체는 Provider별 특수 로직만 구현합니다.
 */
@Slf4j
public abstract class AbstractOAuth2UserMapper implements OAuth2UserMapper {

    private final Provider provider;

    protected AbstractOAuth2UserMapper(Provider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    /**
     * Template Method: 공통 흐름 정의
     * 1. attributes 검증
     * 2. Provider별 특수 로직 실행
     * 3. 예외 처리
     */
    @Override
    public final OAuth2UserAttributes map(OAuth2User oAuth2User) {
        try {
            // 1. 공통 검증
            Map<String, Object> attributes = validateAndGetAttributes(oAuth2User);
            
            // 2. Provider별 특수 로직 실행
            return extractUserAttributes(attributes);
            
        } catch (OAuth2MappingException e) {
            // OAuth2MappingException은 그대로 전파
            throw e;
        } catch (Exception e) {
            // 기타 예외는 매핑 실패로 래핑
            log.error("OAuth2 사용자 정보 매핑 중 예외 발생: provider={}, error={}", 
                    provider, e.getMessage(), e);
            throw OAuth2MappingException.mappingFailed(
                    provider,
                    String.format("%s 사용자 정보 매핑 중 오류가 발생했습니다: %s", 
                            provider, e.getMessage()),
                    e
            );
        }
    }

    /**
     * OAuth2User의 attributes 검증 및 반환
     * 
     * @param oAuth2User OAuth2 사용자 정보
     * @return 검증된 attributes
     * @throws OAuth2MappingException attributes가 null이거나 비어있는 경우
     */
    protected Map<String, Object> validateAndGetAttributes(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        if (attributes == null || attributes.isEmpty()) {
            throw OAuth2MappingException.userInfoEmpty(provider);
        }
        
        return attributes;
    }

    /**
     * Provider별 특수 로직으로 사용자 속성 추출
     * 
     * 각 구현체는 이 메서드만 구현하면 됩니다.
     * 
     * @param attributes 검증된 OAuth2 attributes
     * @return OAuth2UserAttributes
     * @throws OAuth2MappingException 매핑 실패 시
     */
    protected abstract OAuth2UserAttributes extractUserAttributes(Map<String, Object> attributes);

    /**
     * OAuth2UserAttributes Builder 생성 (Provider 자동 설정)
     * 
     * 각 구현체에서 Builder를 생성할 때 사용하는 헬퍼 메서드입니다.
     * Provider는 자동으로 설정되므로 별도로 설정할 필요가 없습니다.
     * 
     * @return OAuth2UserAttributesBuilder (provider가 이미 설정된 상태)
     */
    protected OAuth2UserAttributes.OAuth2UserAttributesBuilder createBuilder() {
        return OAuth2UserAttributes.builder()
                .provider(provider);
    }

    /**
     * Provider ID 검증 및 반환
     * 
     * 각 구현체에서 Provider ID를 추출할 때 사용하는 헬퍼 메서드입니다.
     * null이거나 blank인 경우 예외를 발생시킵니다.
     * 
     * @param providerId Provider ID 문자열
     * @return 검증된 Provider ID
     * @throws OAuth2MappingException providerId가 null이거나 blank인 경우
     */
    protected String validateProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw OAuth2MappingException.providerIdMissing(provider);
        }
        return providerId;
    }

    /**
     * 중첩된 Map 검증 및 반환
     * 
     * Naver, Kakao 등 중첩 구조를 가진 Provider에서 사용하는 헬퍼 메서드입니다.
     * null이거나 비어있는 경우 예외를 발생시킵니다.
     * 
     * @param nestedMap 중첩된 Map
     * @param mapName Map의 이름 (예: "response", "kakao_account") - 예외 메시지용
     * @return 검증된 중첩 Map
     * @throws OAuth2MappingException nestedMap이 null이거나 비어있는 경우
     */
    protected Map<String, Object> validateNestedMap(Map<String, Object> nestedMap, String mapName) {
        if (nestedMap == null || nestedMap.isEmpty()) {
            throw OAuth2MappingException.mappingFailed(
                    provider,
                    String.format("%s %s 정보가 없습니다.", provider, mapName)
            );
        }
        return nestedMap;
    }
}
