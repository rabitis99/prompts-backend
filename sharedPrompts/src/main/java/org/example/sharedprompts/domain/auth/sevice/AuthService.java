package org.example.sharedprompts.domain.auth.sevice;

import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;

public interface AuthService {
    AuthResponseDto signUp(SignUpRequestDto dto);
    TokenResponseDto login(LoginRequestDto dto);
}
