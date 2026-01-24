package org.example.sharedprompts.dto.user.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequestDto {

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    @JsonProperty("current_password")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 10, max = 100, message = "비밀번호는 10자 이상 100자 이하로 입력해주세요.")
    @JsonProperty("new_password")
    private String newPassword;
}