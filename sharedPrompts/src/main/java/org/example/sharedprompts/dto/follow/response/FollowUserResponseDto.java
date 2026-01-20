package org.example.sharedprompts.dto.follow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.dto.user.response.UserTermsResponseDto;

import java.time.LocalDateTime;

/**
 * 팔로워/팔로잉 목록 조회 시 사용하는 DTO
 * UserResponseDto의 모든 필드를 포함하고 양방향 관계 정보를 추가
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserResponseDto {

    // UserResponseDto 필드들
    private Long id;
    private String email;
    private Provider provider;
    private String nickname;
    private Integer age;
    private String job;
    private String thumbnail;
    @JsonProperty("is_signup_completed")
    private boolean isSignupCompleted;
    @JsonProperty("user_terms")
    private UserTermsResponseDto userTerms;

    /**
     * viewer → target 방향 상태
     * 예: 팔로워 목록에서 나(me) → 상대(target)의 팔로우 상태
     */
    @JsonProperty("follow_status")
    private FollowStatus followStatus;

    /**
     * target → viewer 방향 상태 (팔로워 목록에서만 의미있음)
     * 예: 팔로워 목록에서 상대(target) → 나(me)의 팔로우 상태
     */
    @JsonProperty("reverse_follow_status")
    private FollowStatus reverseFollowStatus;

    /**
     * PENDING 상태일 때 요청 방향
     * - me_to_target: 내가 상대에게 요청한 경우
     * - target_to_me: 상대가 나에게 요청한 경우
     */
    @JsonProperty("pending_direction")
    private String pendingDirection;

    /**
     * 내가 상대를 차단했는지 여부
     */
    @JsonProperty("is_blocked_by_me")
    private Boolean isBlockedByMe;

    /**
     * 상대가 나를 차단했는지 여부
     */
    @JsonProperty("is_blocked_by_target")
    private Boolean isBlockedByTarget;

    /**
     * 팔로우 시작일시 (FOLLOWING 상태일 때)
     */
    @JsonProperty("followed_at")
    private LocalDateTime followedAt;

    /**
     * 요청일시 (PENDING 상태일 때)
     */
    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    public static FollowUserResponseDto from(
            User user,
            FollowStatus followStatus,
            FollowStatus reverseFollowStatus,
            Boolean isBlockedByMe,
            Boolean isBlockedByTarget,
            LocalDateTime followedAt,
            LocalDateTime requestedAt
    ) {
        // PENDING 방향 결정
        String pendingDirection = null;
        if (followStatus == FollowStatus.PENDING) {
            pendingDirection = "me_to_target"; // 기본값: 내가 요청한 경우
        } else if (reverseFollowStatus == FollowStatus.PENDING) {
            pendingDirection = "target_to_me"; // 상대가 요청한 경우
        }

        // UserResponseDto 생성 및 필드 복사
        UserResponseDto userDto = UserResponseDto.from(user);
        FollowUserResponseDto.FollowUserResponseDtoBuilder builder = fromUserResponseDto(userDto);

        return builder
                // Follow 관련 필드들
                .followStatus(followStatus)
                .reverseFollowStatus(reverseFollowStatus)
                .pendingDirection(pendingDirection)
                .isBlockedByMe(isBlockedByMe)
                .isBlockedByTarget(isBlockedByTarget)
                .followedAt(followedAt)
                .requestedAt(requestedAt)
                .build();
    }

    /**
     * UserResponseDto의 모든 필드를 FollowUserResponseDto 빌더에 복사
     * UserResponseDto의 매핑 로직 변경 시 이 메서드만 수정하면 일관성 유지 가능
     */
    private static FollowUserResponseDto.FollowUserResponseDtoBuilder fromUserResponseDto(UserResponseDto userDto) {
        return FollowUserResponseDto.builder()
                .id(userDto.getId())
                .email(userDto.getEmail())
                .provider(userDto.getProvider())
                .nickname(userDto.getNickname())
                .age(userDto.getAge())
                .job(userDto.getJob())
                .thumbnail(userDto.getThumbnail())
                .isSignupCompleted(userDto.isSignupCompleted())
                .userTerms(userDto.getUserTerms());
    }
}

