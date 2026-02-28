package org.example.sharedprompts.domain.prompt.application.port.out;

/**
 * LLM 호출 전 사용자 존재 여부를 검증하는 포트.
 *
 * <p>탈퇴·삭제된 유저로 인한 불필요한 LLM 비용을 방지하기 위해
 * {@link SavePromptVersionPort#save}와 책임을 분리한다.
 *
 * @throws org.example.sharedprompts.global.exception.ApiException USER_NOT_FOUND
 */
public interface ValidateUserPort {

    void validateUserExists(Long userId);
}
