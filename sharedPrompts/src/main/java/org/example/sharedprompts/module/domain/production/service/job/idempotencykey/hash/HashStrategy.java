package org.example.sharedprompts.module.domain.production.service.job.idempotencykey.hash;

/**
 * 해시 생성 전략 인터페이스
 * 다양한 해시 알고리즘을 교체 가능하도록 추상화
 */
public interface HashStrategy {

    /**
     * 입력 문자열을 해시하여 16진수 문자열로 반환
     */
    String hash(String input);
}

