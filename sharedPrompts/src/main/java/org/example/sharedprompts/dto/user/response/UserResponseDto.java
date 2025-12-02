package org.example.sharedprompts.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;

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
    private String thumbnail;
    private UserTermsResponseDto userTerms;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
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
