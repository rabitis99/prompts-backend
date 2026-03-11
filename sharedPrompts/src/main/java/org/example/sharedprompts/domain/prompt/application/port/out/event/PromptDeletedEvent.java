package org.example.sharedprompts.domain.prompt.application.port.out.event;

/**
 * "프롬프트 삭제" 이벤트를 위한 페이로드.
 * (Long, Long) 위치 기반 인자 사용 시 promptId와 authorId가 뒤바뀌는 것을 방지하기 위한 전용 타입.
 */
public record PromptDeletedEvent(Long promptId, Long authorId) {}