package org.example.sharedprompts.dto.user.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.User;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요.")
    private String nickname;

    @Min(value = 0, message = "나이는 0 이상으로 입력해주세요.")
    @Max(value = 150, message = "나이는 150 이하로 입력해주세요.")
    private Integer age;

    @Size(max = 100, message = "직업명은 최대 100자까지 입력해주세요.")
    private String job;

    private String thumbnail;

    @JsonProperty("user_terms")
    private UserTermsRequestDto userTerms;

    public void applyTo (User user) {
        if (nickname != null) user.changeNickname(nickname);
        if (age != null) user.updateAge(age);
        if (job != null) user.updateJob(job);
        if (thumbnail != null) user.updateThumbnail(thumbnail);
        if (userTerms != null) {
            user.agreeToTerms(userTerms.toEntity());
        }
        user.changeSignupCompleted(true);
    }
}
