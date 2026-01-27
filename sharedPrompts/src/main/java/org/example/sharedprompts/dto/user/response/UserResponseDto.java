package org.example.sharedprompts.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String email;
    private Provider provider;
    private String nickname;
    private Integer age;
    private String job;
    private Role role;
    private String thumbnail;
    @JsonProperty("is_signup_completed")
    private boolean isSignupCompleted;
    @JsonProperty("user_terms")
    private UserTermsResponseDto userTerms;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .provider(user.getProvider())
                .nickname(user.getNickname())
                .age(user.getAge())
                .job(user.getJob())
                .role(user.getRole())
                .thumbnail(user.getThumbnail())
                .isSignupCompleted(user.isSignupCompleted())
                .userTerms(UserTermsResponseDto.from(user.getTerms()))
                .build();
    }

}
