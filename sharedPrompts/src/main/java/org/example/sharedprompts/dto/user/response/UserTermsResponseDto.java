package org.example.sharedprompts.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.sharedprompts.domain.user.UserTerms;

@Getter
@Builder
@AllArgsConstructor
public class UserTermsResponseDto {

    private final boolean required;
    private final boolean privacy;
    private final boolean marketing;

    public static UserTermsResponseDto from(UserTerms userTerms) {
        return UserTermsResponseDto.builder()
                .required(userTerms.isRequired())
                .privacy(userTerms.isPrivacy())
                .marketing(userTerms.isMarketing())
                .build();
    }
}
