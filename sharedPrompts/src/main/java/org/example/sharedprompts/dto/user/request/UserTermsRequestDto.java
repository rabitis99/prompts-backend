package org.example.sharedprompts.dto.user.request;


import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.user.UserTerms;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTermsRequestDto {

    @AssertTrue(message = "필수 이용약관(required)은 반드시 동의해야 합니다.")
    private boolean required;

    @AssertTrue(message = "개인정보 처리방침(privacy)은 반드시 동의해야 합니다.")
    private boolean privacy;

    private boolean marketing;

    public UserTerms toEntity() {
        return UserTerms.builder()
                .required(required)
                .privacy(privacy)
                .marketing(marketing)
                .build();
    }
}
