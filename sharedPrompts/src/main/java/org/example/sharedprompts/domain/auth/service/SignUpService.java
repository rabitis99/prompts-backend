package org.example.sharedprompts.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.user.service.UserRegistrationService;
import org.example.sharedprompts.domain.user.validator.PasswordStrengthValidator;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 관련 로직을 통합하는 서비스
 * 
 * 비밀번호 검증, 인코딩, 사용자 등록을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthValidator passwordStrengthValidator;
    private final UserRegistrationService userRegistrationService;

    /**
     * 회원가입 처리
     * 
     * @param dto 회원가입 요청 DTO
     * @return 생성된 사용자
     * @throws ApiException 이메일 중복 또는 비밀번호 강도 검증 실패 시
     */
    @Transactional
    public User signUp(SignUpRequestDto dto) {
        // 이메일 중복 체크
        if (userRepository.existsByProviderAndProviderId(Provider.LOCAL, dto.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_EMAIL);
        }

        // 비밀번호 강도 검증
        passwordStrengthValidator.validate(dto.getPassword());

        // 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        
        // 랜덤 닉네임 생성
        String nickname = RandomGenerator.randomNickname();

        // 사용자 등록 (사용자 생성 + tokenVersion 초기화 + 이벤트 발행)
        return userRegistrationService.registerLocalUser(dto, encodedPassword, nickname);
    }
}

