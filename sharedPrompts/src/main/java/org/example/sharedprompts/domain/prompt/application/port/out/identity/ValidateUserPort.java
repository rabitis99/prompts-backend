package org.example.sharedprompts.domain.prompt.application.port.out.identity;

/**
 * LLM 호출 전 사용자 존재 여부를 검증하는 포트.
 *
 * <p>탈퇴·삭제된 유저로 인한 불필요한 LLM 비용을 방지하기 위해
 */
public interface ValidateUserPort {

    void validateUserExists(Long userId);
}
