package org.example.sharedprompts.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequestDto {

    @NotEmpty(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotEmpty(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
    private String password;

    @Size(max = 500, message = "디바이스 토큰은 500자까지 입력해주세요.")
    @JsonProperty("device_token")
    private String deviceToken; // 푸시 알림용 디바이스 토큰 (선택사항)

}
