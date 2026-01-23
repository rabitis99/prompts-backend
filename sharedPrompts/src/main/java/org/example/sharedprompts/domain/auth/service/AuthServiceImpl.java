package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.auth.redis.RefreshTokenMetadata;
import org.example.sharedprompts.auth.redis.RefreshTokenStore;
import org.example.sharedprompts.auth.security.TokenSecurityCheckResult;
import org.example.sharedprompts.auth.security.TokenSecurityValidator;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.auth.service.OAuthLoginFlow.OAuthLoginPayload;
import org.example.sharedprompts.domain.user.PasswordVerifier;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.domain.user.validator.PasswordStrengthValidator;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.domain.user.service.UserRegistrationService;
import org.example.sharedprompts.domain.user.service.UserValidationService;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordVerifier passwordVerifier;
    private final PasswordStrengthValidator passwordStrengthValidator;
    private final UserRegistrationService userRegistrationService;
    private final UserValidationService userValidationService;
    private final AuthAuditPublisher authAuditPublisher;
    private final OAuthLoginFlow oAuthLoginFlow;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenSecurityValidator tokenSecurityValidator;

    @Override
    @Transactional
    public AuthResponseDto signUp(SignUpRequestDto dto) {
        if (userRepository.existsByProviderAndProviderId(Provider.LOCAL, dto.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_EMAIL);
        }

        // 비밀번호 강도 검증
        passwordStrengthValidator.validate(dto.getPassword());

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String nickname = RandomGenerator.randomNickname();

        // UserRegistrationService로 위임 (사용자 생성 + tokenVersion 초기화 + 이벤트 발행)
        User user = userRegistrationService.registerLocalUser(dto, encodedPassword, nickname);
        
        return AuthResponseDto.from(user);
    }

    @Override
    @Transactional
    public TokenResponseDto login(LoginRequestDto dto, HttpServletRequest request) {
        String email = dto.getEmail();

        User user = userRepository.findByProviderAndProviderId(Provider.LOCAL, email)
                .orElseThrow(() -> {
                    log.warn("Login failed - reason=USER_NOT_FOUND, email={}", SensitiveDataMasker.maskEmail(email));

                    authAuditPublisher.loginFailByEmail(Provider.LOCAL, email, AuthFailReason.USER_NOT_FOUND);

                    return new ApiException(ErrorCode.LOGIN_FAILED);
                });

        if (!user.verifyPassword(dto.getPassword(), passwordVerifier)) {
            log.warn("Login failed - reason=INVALID_PASSWORD, email={}", SensitiveDataMasker.maskEmail(email));

            authAuditPublisher.loginFailByUser(user, AuthFailReason.INVALID_PASSWORD);

            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }

        // 사용자 상태 검증 (차단, 삭제 등)
        userValidationService.validate(user);

        TokenResponseDto tokenResponseDto = authTokenService.issue(user, request);

        authAuditPublisher.loginSuccessByUser(user);

        return tokenResponseDto;
    }

    @Override
    @Transactional
    public TokenResponseDto callback(String key, String state, HttpServletRequest request) {
        OAuthLoginPayload payload = oAuthLoginFlow.validate(key, state);

        User user = payload.user();
        // authTokenService.issue()는 Redis에 토큰을 저장하므로 쓰기 작업이 필요합니다.
        TokenResponseDto tokenResponseDto = authTokenService.issue(user, request);

        authAuditPublisher.loginSuccessByUser(user);

        return tokenResponseDto;
    }

    @Override
    @Transactional
    public TokenResponseDto refresh(RefreshRequestDto dto, HttpServletRequest request) {
        // 1. RefreshToken 조회 및 삭제 (1회용 보장)
        RefreshTokenMetadata metadata = refreshTokenStore.getAndDelete(dto.getRefreshToken());
        
        if (metadata == null) {
            authAuditPublisher.tokenRefreshFailWithoutUser(AuthFailReason.INVALID_REFRESH_TOKEN);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        // 2. IP/User-Agent 보안 검증
        TokenSecurityCheckResult checkResult = tokenSecurityValidator.validate(metadata, request);
        handleSecurityCheckResult(checkResult, metadata);
        
        // 3. 사용자 조회
        User user = userRepository.findById(metadata.getUserId())
                .orElseThrow(() -> {
                    authAuditPublisher.tokenRefreshFailByUserId(metadata.getUserId(), AuthFailReason.USER_NOT_FOUND);
                    return new ApiException(ErrorCode.USER_NOT_FOUND);
                });

        // 4. 토큰 재발급 (메타데이터 및 현재 refresh 토큰 전달)
        // getAndDelete로 이미 삭제되었으므로, 회전하지 않는 경우 재저장을 위해 현재 토큰 전달
        TokenResponseDto tokenResponseDto = authTokenService.reissue(
                user, metadata, dto.getRefreshToken(), request
        );

        authAuditPublisher.tokenRefreshSuccessByUser(user);

        return tokenResponseDto;
    }
    
    /**
     * 보안 검증 결과 처리
     */
    private void handleSecurityCheckResult(TokenSecurityCheckResult checkResult, RefreshTokenMetadata metadata) {
        if (checkResult == TokenSecurityCheckResult.MISMATCH) {
            // 보안 로그 + 전체 세션 무효화
            log.warn("Token refresh security alert - userId={}, storedIp={}, storedUa={}",
                    metadata.getUserId(), metadata.getIp(), metadata.getUserAgent());
            refreshTokenStore.deleteAllByUser(metadata.getUserId());
            authAuditPublisher.tokenRefreshFailByUserId(metadata.getUserId(), AuthFailReason.INVALID_REFRESH_TOKEN);
            throw new ApiException(ErrorCode.REFRESH_TOKEN_SECURITY_MISMATCH);
        } else if (checkResult == TokenSecurityCheckResult.SUSPICIOUS) {
            // 경고 로그만
            log.warn("Suspicious token refresh: userId={}, ip={}, ua={}",
                    metadata.getUserId(), metadata.getIp(), metadata.getUserAgent());
        }
    }

    @Override
    @Transactional
    public void logout(Long userId, LogoutRequestDto dto, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    authAuditPublisher.logoutFailByUserId(userId, AuthFailReason.USER_NOT_FOUND);
                    return new ApiException(ErrorCode.USER_NOT_FOUND);
                });

        if (!refreshTokenStore.isValid(dto.getRefreshToken(), userId)) {
            authAuditPublisher.logoutFailByUserId(userId, AuthFailReason.INVALID_REFRESH_TOKEN);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        // Access Token 검증은 TokenRedisService에서 처리
        // (AuthTokenService에서 처리하도록 변경 가능하지만, 기존 구조 유지)

        authTokenService.logout(user, accessToken, dto.getRefreshToken());
        authAuditPublisher.logoutSuccessByUser(user);
    }
}

