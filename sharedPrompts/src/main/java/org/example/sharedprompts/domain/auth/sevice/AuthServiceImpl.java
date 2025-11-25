package org.example.sharedprompts.domain.auth.sevice;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponseDto signUp(SignUpRequestDto dto) {

        // 이메일 중복 검사
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_EMAIL);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // 랜덤 닉네임 (임시)
        String nickname = "user_" + UUID.randomUUID()
                .toString().substring(0, 8);

        // 유저 생성 및 저장
        User user = userRepository.save(
                dto.toEntity(encodedPassword, nickname)
        );

        // 응답 반환
        return AuthResponseDto.from(user);
    }
}
