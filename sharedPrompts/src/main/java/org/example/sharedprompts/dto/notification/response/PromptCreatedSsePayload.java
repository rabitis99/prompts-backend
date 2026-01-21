package org.example.sharedprompts.dto.notification.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프롬프트 생성 SSE 알림용 페이로드
 *
 * - promptId
 * - authorId
 * - authorNickname
 * - promptSummary (전문이 아닌 요약/description)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCreatedSsePayload {

    private Long promptId;
    private Long authorId;
    private String authorNickname;
    private String promptSummary;
}


