package org.example.sharedprompts.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.constant.Constant;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.global.jwt.JwtProvider;
import org.example.sharedprompts.global.redis.TokenRedisService;
import org.example.sharedprompts.global.util.RandomGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;

    @Override
    public AuthResponseDto signUp(SignUpRequestDto dto) {
        if (userRepository.existsByProviderAndEmail(Provider.LOCAL, dto.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String nickname = RandomGenerator.randomNickname();

        User user = userRepository.save(dto.toEntity(encodedPassword, nickname));
        return AuthResponseDto.from(user);
    }

    @Override
    public TokenResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByProviderAndEmail(Provider.LOCAL, dto.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        TokenResponseDto tokenResponseDto = jwtProvider.getToken(user);

        tokenRedisService.saveAccessToken(tokenResponseDto.getAccessToken(), user.getId());
        tokenRedisService.saveRefreshToken(tokenResponseDto.getRefreshToken(), user.getId());

        return tokenResponseDto;
    }

    @Override
    public TokenResponseDto callback(String code, String state) {

        // 1. 임시 Redis에서 토큰 정보 가져오기
        Map<String, String> tokens = tokenRedisService.getAndDeleteTempToken(code);
        if (tokens == null) {
            throw new ApiException(ErrorCode.OAUTH2_INVALID_CODE);
        }

        // 2. state 검증
        String expectedState = tokens.get(Constant.STATE_KEY);
        if (expectedState == null || !expectedState.equals(state)) {
            throw new ApiException(ErrorCode.OAUTH2_STATE_MISMATCH);
        }

        // 3. access, refresh 토큰 존재 확인
        String accessToken = tokens.get(Constant.ACCESS_TOKEN_KEY);
        String refreshToken = tokens.get(Constant.REFRESH_TOKEN_KEY);
        if (accessToken == null || refreshToken == null) {
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_EXPIRED);
        }

        // 4. userId를 안전하게 확보 (DB 조회)
        String providerStr = tokens.get("provider"); // OAuth2 발급 시 provider 저장 필요
        String providerId = tokens.get("providerId"); // OAuth2 발급 시 providerId 저장 필요
        if (providerStr == null || providerId == null) {
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_INVALID);
        }

        Provider provider;
        try {
            provider = Provider.valueOf(providerStr);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_INVALID, "유효하지 않은 provider 값");
        }

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 5. Redis에 실제 Access/Refresh Token 저장
        tokenRedisService.saveAccessToken(accessToken, user.getId());
        tokenRedisService.saveRefreshToken(refreshToken, user.getId());

        // 6. 클라이언트에 토큰 반환
        return new TokenResponseDto(accessToken, refreshToken);
    }

}
