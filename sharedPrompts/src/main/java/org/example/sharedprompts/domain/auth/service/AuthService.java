package org.example.sharedprompts.domain.auth.service;

import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;

public interface AuthService {
    AuthResponseDto signUp(SignUpRequestDto dto);
    TokenResponseDto login(LoginRequestDto dto);
    TokenResponseDto callback(String code, String state);
    TokenResponseDto refresh(Long userId, RefreshRequestDto dto);
    void logout(Long userId, LogoutRequestDto dto);
}
