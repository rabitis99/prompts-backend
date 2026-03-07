package org.example.sharedprompts.domain.prompt.application.port.out.identity;

/**
 * LLM 호출 전 사용자 존재 여부를 검증하는 포트.
 *
 * <p>탈퇴·삭제된 유저로 인한 불필요한 LLM 비용을 방지한다.
 * 사용자가 없으면 예외를 던진다.</p>
 */
public interface ValidateUserPort {

    /**
     * 사용자가 존재하는지 검증한다. 없으면 예외.
     */
    void validateUserExists(Long userId);
}
