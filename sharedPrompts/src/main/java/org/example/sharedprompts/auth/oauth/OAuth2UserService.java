package org.example.sharedprompts.auth.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.user.service.UserRegistrationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OAuth2 사용자 서비스
 * 
 * OAuth2 사용자 조회 및 생성을 담당하는 Application Service입니다.
 * 사용자 조회/생성 로직을 OAuth2 인증 흐름에서 분리하여
 * 도메인 로직의 재사용성과 테스트 용이성을 향상시킵니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService {
    private final UserRepository userRepository;
    private final UserRegistrationService userRegistrationService;
    
    /**
     * OAuth2 사용자 조회 또는 생성
     * 
     * 기존 사용자가 있으면 조회하고, 없으면 신규 등록합니다.
     * 
     * 동시성 처리:
     * - 동시에 같은 OAuth 계정으로 로그인 시도 시, 한 건만 성공하고 나머지는 재조회
     * - DB Unique 제약 조건(provider + providerId)으로 중복 방지
     * 
     * @param provider OAuth2 제공자
     * @param attributes OAuth2 사용자 속성
     * @return 사용자 (기존 또는 신규)
     */
    @Transactional
    public User findOrCreateUser(Provider provider, OAuth2UserAttributes attributes) {
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(
                provider, 
                attributes.getProviderId()
        );
        
        if (existingUser.isPresent()) {
            log.debug("기존 OAuth2 사용자 조회: userId={}, provider={}", 
                    existingUser.get().getId(), provider);
            return existingUser.get();
        }
        
        try {
            return userRegistrationService.registerOAuth2User(provider, attributes);
        } catch (DataIntegrityViolationException e) {
            // 동시성 이슈: 다른 요청이 먼저 사용자를 생성한 경우 재조회
            log.debug("OAuth2 사용자 등록 중 중복 감지, 재조회: provider={}, providerId={}", 
                    provider, attributes.getProviderId());
            
            return userRepository.findByProviderAndProviderId(provider, attributes.getProviderId())
                    .orElseThrow(() -> {
                        log.error("OAuth2 사용자 재조회 실패: provider={}, providerId={}", 
                                provider, attributes.getProviderId());
                        return new IllegalStateException("사용자 등록 및 조회 실패", e);
                    });
        }
    }
}

