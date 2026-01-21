package org.example.sharedprompts.domain.notification.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * RabbitMQ를 통해 전송되는 프롬프트 생성 SSE 알림 메시지 DTO
 * - 프롬프트 생성 시 팔로워들에게 SSE 알림을 전송하기 위한 메시지
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCreatedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 프롬프트 ID
     */
    @NotNull(message = "프롬프트 ID는 필수입니다")
    @JsonProperty("prompt_id")
    private Long promptId;

    /**
     * 프롬프트 작성자 ID
     */
    @NotNull(message = "작성자 ID는 필수입니다")
    @JsonProperty("author_id")
    private Long authorId;

    /**
     * 작성자 닉네임
     */
    @NotNull(message = "작성자 닉네임은 필수입니다")
    @JsonProperty("author_nickname")
    private String authorNickname;

    /**
     * 프롬프트 요약 (description)
     */
    @JsonProperty("prompt_summary")
    private String promptSummary;

    /**
     * SSE 알림을 받을 팔로워 ID 리스트
     */
    @NotEmpty(message = "알림 대상 사용자 리스트는 필수입니다")
    @JsonProperty("follower_ids")
    private List<Long> followerIds;
}

