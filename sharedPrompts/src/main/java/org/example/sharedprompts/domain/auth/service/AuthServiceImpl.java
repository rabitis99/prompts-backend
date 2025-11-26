package org.example.sharedprompts.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
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

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        String nickname = RandomGenerator.randomNickname();

        User user = userRepository.save(
                dto.toEntity(encodedPassword, nickname)
        );

        return AuthResponseDto.from(user);
    }

    @Override
    public TokenResponseDto login(LoginRequestDto dto) {

        User user = userRepository.findByEmail(dto.getEmail())
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

        Map<String, String> tokens = tokenRedisService.getAndDeleteTempToken(code);

        if (tokens == null) {
            throw new ApiException(ErrorCode.OAUTH2_INVALID_CODE);
        }

        String expectedState = tokens.get("state"); // 이전에 저장한 state
        if (expectedState == null || !expectedState.equals(state)) {
            throw new ApiException(ErrorCode.OAUTH2_STATE_MISMATCH);
        }

        String accessToken = tokens.get("access_token");
        String refreshToken = tokens.get("refresh_token");

        if (accessToken == null || refreshToken == null) {
            throw new ApiException(ErrorCode.OAUTH2_TOKEN_EXPIRED);
        }

        return new TokenResponseDto(accessToken, refreshToken);
    }

}
