package org.example.sharedprompts.auth.oauth.mapper;

import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 사용자 정보 매핑 인터페이스
 * 
 * Provider별로 OAuth2User의 attributes를 OAuth2UserAttributes로 변환합니다.
 */
public interface OAuth2UserMapper {
    
    /**
     * 이 Mapper가 처리하는 Provider 반환
     * 
     * @return Provider
     */
    Provider getProvider();
    
    /**
     * OAuth2User의 attributes를 OAuth2UserAttributes로 변환
     * 
     * @param oAuth2User OAuth2 사용자 정보
     * @return 변환된 사용자 속성
     */
    OAuth2UserAttributes map(OAuth2User oAuth2User);
}
