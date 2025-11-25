package org.example.sharedprompts.domain.auth.sevice;

import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;

public interface AuthService {
    AuthResponseDto signUp(SignUpRequestDto dto);
}
