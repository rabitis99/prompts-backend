package org.example.sharedprompts.dto.follow.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.follow.FollowStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponseDto {

    /**
     * viewer → target 방향 상태
     */
    @JsonProperty("status")
    private String status;

    /**
     * target → viewer 방향 상태
     */
    @JsonProperty("reverse_status")
    private String reverseStatus;

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
     * 생성일시
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static FollowResponseDto from(FollowStatus status) {
        return FollowResponseDto.builder()
                .status(status != null ? status.name() : null)
                .build();
    }

    public static FollowResponseDto from(
            Follow follow,
            Follow reverseFollow
    ) {
        FollowStatus followStatus = follow != null ? follow.getStatus() : null;
        FollowStatus reverseStatus = reverseFollow != null ? reverseFollow.getStatus() : null;

        // PENDING 방향 결정
        String pendingDirection = null;
        if (followStatus == FollowStatus.PENDING && follow != null) {
            // follow가 존재하고 PENDING이면, viewer가 요청한 것
            pendingDirection = "me_to_target";
        } else if (reverseStatus == FollowStatus.PENDING && reverseFollow != null) {
            // reverseFollow가 존재하고 PENDING이면, target이 요청한 것
            pendingDirection = "target_to_me";
        }

        // 블록 상태 확인
        Boolean isBlockedByMe = followStatus == FollowStatus.BLOCKED;
        Boolean isBlockedByTarget = reverseStatus == FollowStatus.BLOCKED;

        // 사용할 Follow 엔티티 (follow 또는 reverseFollow 중 하나)
        Follow mainFollow = follow != null ? follow : reverseFollow;

        return FollowResponseDto.builder()
                .status(followStatus != null ? followStatus.name() : null)
                .reverseStatus(reverseStatus != null ? reverseStatus.name() : null)
                .pendingDirection(pendingDirection)
                .isBlockedByMe(isBlockedByMe)
                .isBlockedByTarget(isBlockedByTarget)
                .createdAt(mainFollow != null ? mainFollow.getCreatedAt() : null)
                .updatedAt(mainFollow != null ? mainFollow.getUpdatedAt() : null)
                .build();
    }
}

