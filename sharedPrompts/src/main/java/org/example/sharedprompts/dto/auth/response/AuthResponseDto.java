package org.example.sharedprompts.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.dto.user.response.UserTermsResponseDto;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private Long id;
    private String email;
    private Provider provider;
    private String nickname;
    private Integer age;
    private String job;
    private String thumbnail;
    private UserTermsResponseDto userTerms;

    public static AuthResponseDto from(User user) {
        return AuthResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .provider(user.getProvider())
                .nickname(user.getNickname())
                .age(user.getAge())
                .job(user.getJob())
                .thumbnail(user.getThumbnail())
                .userTerms(UserTermsResponseDto.from(user.getTerms()))
                .build();
    }
}
