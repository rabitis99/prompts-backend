package org.example.sharedprompts.domain.prompt.application.port.out.event;

/**
 * "프롬프트 조회" 이벤트를 위한 페이로드.
 * (Long, Long) 형태의 위치 기반 인자를 사용할 때 promptId와 viewerId가 뒤바뀌는 것을 방지하기 위한 전용 타입.
 */
public record PromptViewedEvent(Long promptId, Long viewerId) {}