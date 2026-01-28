package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.PasswordVerifier;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.user.service.UserValidationService;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 관련 로직을 통합하는 서비스
 * 
 * 사용자 조회, 비밀번호 검증, 사용자 상태 검증을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordVerifier passwordVerifier;
    private final UserValidationService userValidationService;
    private final AuthAuditPublisher authAuditPublisher;

    /**
     * 로그인 인증 처리
     * 
     * @param dto 로그인 요청 DTO
     * @param request HTTP 요청 (감사 로그용)
     * @return 인증된 사용자
     * @throws ApiException 사용자 없음, 비밀번호 불일치, 사용자 상태 검증 실패 시
     */
    @Transactional(readOnly = true)
    public User authenticate(LoginRequestDto dto, HttpServletRequest request) {
        String email = dto.getEmail();

        // 사용자 조회
        User user = userRepository.findByProviderAndProviderId(Provider.LOCAL, email)
                .orElseThrow(() -> {
                    log.warn("Login failed - reason=USER_NOT_FOUND, email={}", SensitiveDataMasker.maskEmail(email));
                    authAuditPublisher.loginFailByEmail(Provider.LOCAL, email, AuthFailReason.USER_NOT_FOUND);
                    return new ApiException(ErrorCode.LOGIN_FAILED);
                });

        // 비밀번호 검증
        if (!user.verifyPassword(dto.getPassword(), passwordVerifier)) {
            log.warn("Login failed - reason=INVALID_PASSWORD, email={}", SensitiveDataMasker.maskEmail(email));
            authAuditPublisher.loginFailByUser(user, AuthFailReason.INVALID_PASSWORD, request);
            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }

        // 사용자 상태 검증 (차단, 삭제 등)
        userValidationService.validate(user);

        return user;
    }
}

