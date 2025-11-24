package org.example.sharedprompts.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "providerId")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(length = 200, unique = true)
    private String providerId;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    private Integer age;

    @Column(length = 100)
    private String job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(columnDefinition = "TEXT")
    private String thumbnail;

    @Embedded
    private UserTerms terms;

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void agreeMarketing() {
        if (this.terms == null) {
            this.terms = UserTerms.ofDefault().agreeMarketing();
        } else {
            this.terms = this.terms.agreeMarketing();
        }
    }

    public boolean hasAgreedRequiredTerms() {
        return terms != null && terms.isAllRequiredAgreed();
    }
}
