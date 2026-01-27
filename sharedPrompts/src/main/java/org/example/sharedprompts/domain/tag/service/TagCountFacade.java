package org.example.sharedprompts.domain.tag.service;

import java.util.Set;

/**
 * 태그 카운트 관련 작업을 통합하는 파사드 인터페이스
 * - PromptTagServiceImpl이 태그 카운트 관련 복잡성을 숨기고 단순한 인터페이스로 접근
 */
public interface TagCountFacade {

    /**
     * 단일 태그 카운트 즉시 증가
     * 트랜잭션 내부에서 호출 시 즉시 처리
     * 
     * @param tagName 태그 이름
     */
    void incrementTagCount(String tagName);

    /**
     * 태그 카운트 업데이트 이벤트 발행
     * 트랜잭션 커밋 후 비동기로 처리되거나, 설정에 따라 동기 처리
     * 
     * @param tagsToDecrease 감소할 태그 목록
     * @param tagsToIncrease 증가할 태그 목록
     */
    void publishTagCountUpdate(Set<String> tagsToDecrease, Set<String> tagsToIncrease);
}

