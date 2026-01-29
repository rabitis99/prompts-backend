package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.audit.AuthAuditPublisher;
import org.example.sharedprompts.auth.storage.RefreshTokenMetadata;
import org.example.sharedprompts.auth.storage.RefreshTokenStore;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.auth.service.OAuthLoginFlow.OAuthLoginPayload;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 구현체
 * 
 * 회원가입, 로그인, 토큰 갱신, 로그아웃 등의 인증 흐름을 조율합니다.
 * 세부 로직은 SignUpService, LoginService, TokenSecurityService 등으로 위임합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthAuditPublisher authAuditPublisher;
    private final OAuthLoginFlow oAuthLoginFlow;
    private final AuthTokenService authTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final SignUpService signUpService;
    private final LoginService loginService;
    private final TokenSecurityService tokenSecurityService;

    @Override
    @Transactional
    public AuthResponseDto signUp(SignUpRequestDto dto) {
        User user = signUpService.signUp(dto);
        return AuthResponseDto.from(user);
    }

    @Override
    @Transactional
    public TokenResponseDto login(LoginRequestDto dto, HttpServletRequest request) {
        // 로그인 인증 (감사 로그는 LoginService에서 처리)
        User user = loginService.authenticate(dto, request);

        // deviceToken 업데이트 (제공된 경우)
        if (dto.getDeviceToken() != null && !dto.getDeviceToken().isEmpty()) {
            user.updateDeviceToken(dto.getDeviceToken());
            userRepository.save(user);
            log.debug("로그인 시 deviceToken 업데이트: userId={}", user.getId());
        }

        // 로그인 성공 처리
        TokenResponseDto tokenResponseDto = authTokenService.issue(user, request);
        authAuditPublisher.loginSuccessByUser(user, request);

        return tokenResponseDto;
    }

    @Override
    @Transactional
    public TokenResponseDto callback(String key, String state, String deviceToken, HttpServletRequest request) {
        OAuthLoginPayload payload = oAuthLoginFlow.validate(key, state);

        User user = payload.user();
        
        // deviceToken 업데이트 (제공된 경우)
        if (deviceToken != null && !deviceToken.isEmpty()) {
            user.updateDeviceToken(deviceToken);
            userRepository.save(user);
            log.debug("OAuth 로그인 시 deviceToken 업데이트: userId={}", user.getId());
        }
        
        // authTokenService.issue()는 Redis에 토큰을 저장하므로 쓰기 작업이 필요합니다.
        TokenResponseDto tokenResponseDto = authTokenService.issue(user, request);

        authAuditPublisher.loginSuccessByUser(user, request);

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
        
        // 2. IP/User-Agent 보안 검증 및 결과 처리
        tokenSecurityService.validateAndHandle(metadata, request);
        
        // 3. 사용자 조회
        User user = userRepository.findById(metadata.getUserId())
                .orElseThrow(() -> {
                    authAuditPublisher.tokenRefreshFailByUserId(metadata.getUserId(), AuthFailReason.USER_NOT_FOUND, request);
                    return new ApiException(ErrorCode.USER_NOT_FOUND);
                });

        // 4. 토큰 재발급 (메타데이터 및 현재 refresh 토큰 전달)
        // getAndDelete로 이미 삭제되었으므로, 회전하지 않는 경우 재저장을 위해 현재 토큰 전달
        TokenResponseDto tokenResponseDto = authTokenService.reissue(
                user, metadata, dto.getRefreshToken(), request
        );

        authAuditPublisher.tokenRefreshSuccessByUser(user, request);

        return tokenResponseDto;
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

