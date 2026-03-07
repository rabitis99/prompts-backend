package org.example.sharedprompts.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.global.entity.BaseEntity;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "providerId"})
        },
        indexes = {
                // provider + providerId + deleted_at: 로그인/존재 여부 조회 최적화
                @Index(name = "idx_users_provider_provider_id_deleted_at", columnList = "provider, provider_id, deleted_at"),
                // 최근 활동/가입자 수 통계를 위한 인덱스
                @Index(name = "idx_users_updated_at_deleted_at", columnList = "updated_at, deleted_at"),
                @Index(name = "idx_users_created_at_deleted_at", columnList = "created_at, deleted_at"),
                // 관리자/권한 관련 조회 및 보호 로직
                @Index(name = "idx_users_role_deleted_at", columnList = "role, deleted_at"),
                // 추가된 인덱스 목록 (우선순위: 필수)
                // 활성 사용자 필터링 최적화 (팔로워/팔로잉 목록 조회 시 필수)
                @Index(name = "idx_users_blocked_deleted_at", columnList = "blocked, deleted_at")
        }
)
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
    @Builder.Default
    private boolean signupCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserTier tier = UserTier.FREE;

    @Column(length = 500)
    private String deviceToken; // 푸시 알림용 디바이스 토큰 (FCM 등)

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public void changeTier(UserTier tier) {
        this.tier = tier;
    }

    public void updateDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 비밀번호 검증 로직을 엔티티 안으로 모읍니다.
     * 인코딩/매칭 구현은 PasswordVerifier에 위임합니다.
     */
    public boolean verifyPassword(String rawPassword, PasswordVerifier verifier) {
        if (this.password == null) {
            return false;
        }
        return verifier.matches(rawPassword, this.password);
    }

}
