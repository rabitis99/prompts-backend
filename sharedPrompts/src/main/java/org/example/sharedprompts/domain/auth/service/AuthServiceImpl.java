package org.example.sharedprompts.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;

    @Override
    @Transactional
    public AuthResponseDto signUp(SignUpRequestDto dto) {
        if (userRepository.existsByProviderAndProviderId(Provider.LOCAL, dto.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String nickname = RandomGenerator.randomNickname();

        User user = userRepository.save(dto.toEntity(encodedPassword, nickname));
        return AuthResponseDto.from(user);
    }

    @Override
    @Transactional
    public TokenResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByProviderAndProviderId(Provider.LOCAL, dto.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));

        if (user.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        TokenResponseDto tokenResponseDto = jwtProvider.getToken(user);

        tokenRedisService.saveAccessToken(tokenResponseDto.getAccessToken(), user.getId());
        tokenRedisService.saveRefreshToken(tokenResponseDto.getRefreshToken(), user.getId());

        return tokenResponseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto callback(String key, String state) {

        Map<String, String> tokens = tokenRedisService.getAndDeleteTempToken(key);

        if (tokens == null) {
            throw new ApiException(ErrorCode.OAUTH2_INVALID_CODE);
        }

        String expectedState = tokens.get(Constant.STATE_KEY);
        if (expectedState == null || !expectedState.equals(state)) {
            throw new ApiException(ErrorCode.OAUTH2_STATE_MISMATCH);
        }

        String accessToken = tokens.get(Constant.ACCESS_TOKEN_KEY);
        String refreshToken = tokens.get(Constant.REFRESH_TOKEN_KEY);
        String providerStr = tokens.get(Constant.PROVIDER_KEY);
        String providerId = tokens.get(Constant.PROVIDER_ID_KEY);

        if (accessToken == null || refreshToken == null || providerStr == null || providerId == null) {
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_INVALID);
        }

        // userId 조회
        User user = userRepository.findByProviderAndProviderId(
                Provider.valueOf(providerStr),
                providerId
        ).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));


        tokenRedisService.saveAccessToken(accessToken, user.getId());
        tokenRedisService.saveRefreshToken(refreshToken, user.getId());

        return new TokenResponseDto(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public TokenResponseDto refresh(RefreshRequestDto dto) {

        Long userId = tokenRedisService.getRefreshToken(dto.getRefreshToken());

        if (userId == null) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!tokenRedisService.isRefreshTokenValid(dto.getRefreshToken(), userId)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        tokenRedisService.deleteRefreshToken(dto.getRefreshToken(), userId);

        tokenRedisService.saveAccessToken(newAccessToken, userId);
        tokenRedisService.saveRefreshToken(newRefreshToken, userId);


        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(Long userId, LogoutRequestDto dto, String accessToken) {

        if (!tokenRedisService.isRefreshTokenValid(dto.getRefreshToken(), userId)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!tokenRedisService.isAccessTokenValidWithUserId(accessToken, userId)) {
            throw new ApiException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        tokenRedisService.deleteAccessToken(accessToken);
        tokenRedisService.deleteRefreshToken(dto.getRefreshToken(), userId);

    }

}
