package org.example.sharedprompts.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequestDto {

    @NotBlank(message = "리프레시 토큰을 입력해주세요.")
    @Size(max = 500, message = "리프레시 토큰은 500자까지 입력해주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_.+/=]*$",
            message = "유효하지 않은 토큰 형식입니다."
    )
    @JsonProperty("refresh_token")
    private String refreshToken;
}

