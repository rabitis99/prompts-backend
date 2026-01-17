package org.example.sharedprompts.dto.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.enums.Role;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleChangeRequestDto {

    @NotNull(message = "권한은 필수입니다.")
    @JsonProperty("role")
    private Role role;
}

