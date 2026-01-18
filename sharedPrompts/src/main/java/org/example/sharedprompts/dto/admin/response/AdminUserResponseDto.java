package org.example.sharedprompts.dto.admin.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDto {

    private Long id;
    private String email;
    private Provider provider;
    private String nickname;
    private Role role;
    @JsonProperty("is_blocked")
    private boolean blocked;
    @JsonProperty("is_signup_completed")
    private boolean signupCompleted;
    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static AdminUserResponseDto from(User user) {
        return AdminUserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .provider(user.getProvider())
                .nickname(user.getNickname())
                .role(user.getRole())
                .blocked(user.isBlocked())
                .signupCompleted(user.isSignupCompleted())
                .deletedAt(user.getDeletedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

