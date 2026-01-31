package org.example.sharedprompts.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(max = 512, message = "deviceToken은 최대 512자까지 가능합니다.")
    private String deviceToken; // 푸시 알림용 디바이스 토큰 (선택사항)
}

