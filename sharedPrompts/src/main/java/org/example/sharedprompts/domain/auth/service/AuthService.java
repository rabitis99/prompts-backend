package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;

public interface AuthService {
    AuthResponseDto signUp(SignUpRequestDto dto);
    TokenResponseDto login(LoginRequestDto dto, HttpServletRequest request);
    TokenResponseDto callback(String key, String state, HttpServletRequest request);
    TokenResponseDto refresh(RefreshRequestDto dto, HttpServletRequest request);
    void logout(Long userId, LogoutRequestDto dto, String accessToken);
}
