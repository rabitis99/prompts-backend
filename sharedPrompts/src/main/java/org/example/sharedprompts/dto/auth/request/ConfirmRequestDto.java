package org.example.sharedprompts.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConfirmRequestDto {

    @NotEmpty(message = "tempKey를 입력해주세요.")
    @JsonProperty("temp_key")
    private String tempKey;

    @NotEmpty(message = "state를 입력해주세요.")
    private String state;

    @JsonProperty("device_token")
    private String deviceToken; // 푸시 알림용 디바이스 토큰 (선택사항)
}

