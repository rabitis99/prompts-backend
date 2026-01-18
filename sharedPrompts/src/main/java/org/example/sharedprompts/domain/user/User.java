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
        @UniqueConstraint(columnNames = {"provider", "providerId"})
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = false, length = 200)
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

    @Column(nullable = false)
    private boolean signupCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void agreeToTerms(UserTerms terms) {
        this.terms = terms;
    }

    public void updateAge(Integer age) {
        this.age = age;
    }

    public void updateJob(String job) {
        this.job = job;
    }

    public void changeSignupCompleted(boolean signupCompleted) {
        this.signupCompleted = signupCompleted;
    }

    public void block() {
        this.blocked = true;
    }

    public void unblock() {
        this.blocked = false;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void softDelete() {
        this.deletedAt = java.time.LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}
