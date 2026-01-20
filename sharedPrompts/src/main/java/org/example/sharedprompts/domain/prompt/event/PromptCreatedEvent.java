package org.example.sharedprompts.domain.prompt.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프롬프트 생성 도메인 이벤트
 * - 트랜잭션 커밋 이후(SSE 등) 후속 작업을 위해 사용
 */
@Getter
@RequiredArgsConstructor
public class PromptCreatedEvent {

    private final Long promptId;
    private final Long authorId;
    private final String authorNickname;
    /**
     * 프롬프트 요약(전체 내용이 아닌 description 정도만 사용)
     */
    private final String promptSummary;
}


