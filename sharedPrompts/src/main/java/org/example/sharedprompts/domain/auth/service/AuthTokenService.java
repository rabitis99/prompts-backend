package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.auth.jwt.config.TokenTtlProperties;
import org.example.sharedprompts.auth.jwt.service.JwtTokenService;
import org.example.sharedprompts.auth.redis.RefreshTokenMetadata;
import org.example.sharedprompts.auth.redis.RefreshTokenStore;
import org.example.sharedprompts.auth.security.RequestContext;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.springframework.stereotype.Service;


/**
 * 인증 토큰 발급 및 Redis 저장을 담당하는 서비스
 *
 * - access/refresh 토큰 생성
 * - Redis 저장/갱신 책임을 한 곳으로 모아둡니다.
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenService jwtTokenService;
    private final TokenRedisService tokenRedisService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenTtlProperties ttlProperties;

    /**
     * 새로운 토큰 발급 + Redis 저장
     * 
     * @param user 사용자
     * @param request HTTP 요청 (IP/User-Agent 추출용)
     */
    public TokenResponseDto issue(User user, HttpServletRequest request) {
        TokenResponseDto token = jwtTokenService.getToken(user);
        RequestContext context = RequestContext.from(request);

        tokenRedisService.saveAccessToken(token.getAccessToken(), user.getId());
        refreshTokenStore.save(token.getRefreshToken(), user.getId(), context.getIp(), context.getUserAgent());

        return token;
    }

    /**
     * 토큰 재발급 (Refresh Token 회전 전략 적용)
     * 
     * @param user 사용자 (null이 아니어야 함)
     * @param metadata Refresh Token 메타데이터 (null이 아니어야 함, 이미 검증 및 조회 완료)
     * @param request HTTP 요청 (IP/User-Agent 추출용)
     * @return 새로운 토큰 (Refresh Token은 조건부 재발급)
     * @throws ApiException user 또는 metadata가 null인 경우
     */
    public TokenResponseDto reissue(User user, RefreshTokenMetadata metadata, HttpServletRequest request) {
        // 방어적 프로그래밍: null 체크 (이미 AuthServiceImpl에서 검증되었지만 안전장치)
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (metadata == null) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        // 1. 남은 TTL 계산
        long remainingTtlMillis = metadata.getRemainingTtlMillis();
        long thresholdMillis = ttlProperties.getRefreshTokenRotationThresholdMillis();
        
        // 2. 재발급 전략 결정
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        
        String newRefreshToken = null;
        if (remainingTtlMillis < thresholdMillis) {
            // Access + Refresh 둘 다 재발급
            newRefreshToken = jwtTokenService.generateRefreshToken(user);
            RequestContext context = RequestContext.from(request);
            refreshTokenStore.save(newRefreshToken, user.getId(), context.getIp(), context.getUserAgent());
        }
        // else: Access만 재발급
        
        tokenRedisService.saveAccessToken(newAccessToken, user.getId());
        
        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 시 토큰 삭제
     * 
     * @param user 사용자 (null이면 아무 작업도 수행하지 않음)
     * @param accessToken Access Token (null 가능)
     * @param refreshToken Refresh Token (null 가능)
     */
    public void logout(User user, String accessToken, String refreshToken) {
        if (user == null) {
            return;
        }
        Long userId = user.getId();

        if (accessToken != null) {
            tokenRedisService.deleteAccessToken(accessToken);
        }
        if (refreshToken != null) {
            refreshTokenStore.delete(refreshToken, userId);
        }
    }
}


