package org.example.sharedprompts.dto.auth.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequestDto {
    @NotEmpty(message = "액세스 토큰은 필수입니다.")
    private String accessToken;
    @NotEmpty(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
