package org.example.sharedprompts.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.oauth.mapper.model.OAuth2UserAttributes;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.UserTerms;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.event.UserRegisteredEvent;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 등록 서비스
 * 
 * 사용자 생성 및 이벤트 발행을 담당합니다.
 * tokenVersion 초기화는 UserSecurityEvents를 통해 처리됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserSecurityEvents userSecurityEvents;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 로컬 사용자 등록
     * 
     * @param dto 회원가입 요청 DTO
     * @param encodedPassword 인코딩된 비밀번호
     * @param nickname 닉네임
     * @return 생성된 사용자
     */
    @Transactional
    public User registerLocalUser(SignUpRequestDto dto, String encodedPassword, String nickname) {
        User user = dto.toEntity(encodedPassword, nickname);
        User savedUser = saveAndInitializeUser(user);
        
        log.info("로컬 사용자 등록 완료: userId={}, email={}", 
                savedUser.getId(), SensitiveDataMasker.maskEmail(savedUser.getEmail()));
        
        return savedUser;
    }

    /**
     * OAuth2 사용자 등록
     * 
     * @param provider OAuth2 제공자
     * @param attributes OAuth2 사용자 속성
     * @return 생성된 사용자
     */
    @Transactional
    public User registerOAuth2User(Provider provider, OAuth2UserAttributes attributes) {
        User user = User.builder()
                .email(attributes.getEmail())
                .provider(provider)
                .providerId(attributes.getProviderId())
                .nickname(attributes.getNickname())
                .thumbnail(attributes.getImageUrl())
                .terms(UserTerms.ofDefault())
                .role(Role.ROLE_USER)
                .signupCompleted(false)
                .build();
        
        User savedUser = saveAndInitializeUser(user);
        
        log.info("OAuth2 사용자 등록 완료: userId={}, provider={}, email={}", 
                savedUser.getId(), provider, SensitiveDataMasker.maskEmail(savedUser.getEmail()));
        
        return savedUser;
    }

    /**
     * 사용자 저장, tokenVersion 초기화, 이벤트 발행을 수행하는 공통 메서드
     * 
     * @param user 저장할 사용자 엔티티
     * @return 저장된 사용자
     */
    private User saveAndInitializeUser(User user) {
        User savedUser = userRepository.save(user);
        
        // tokenVersion 초기화 (UserSecurityEvents를 통해 처리)
        userSecurityEvents.onUserRegistered(savedUser.getId());
        
        // 이벤트 발행
        eventPublisher.publishEvent(
                UserRegisteredEvent.of(
                        savedUser.getId(),
                        savedUser.getProvider(),
                        savedUser.getEmail(),
                        savedUser.getNickname()
                )
        );
        
        return savedUser;
    }
}
