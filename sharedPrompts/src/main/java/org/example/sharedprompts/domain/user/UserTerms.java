package org.example.sharedprompts.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserTerms {

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean privacy;

    @Column(nullable = false)
    private boolean marketing = false;

    public boolean isAllRequiredAgreed() {
        return required && privacy;
    }

    public UserTerms agreeMarketing() {
        return UserTerms.builder()
                .required(this.required)
                .privacy(this.privacy)
                .marketing(true)
                .build();
    }

    public static UserTerms ofDefault() {
        return UserTerms.builder()
                .required(false)
                .privacy(false)
                .marketing(false)
                .build();
    }
}
