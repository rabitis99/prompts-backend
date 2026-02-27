package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;

/**
 * 생성된 프롬프트를 저장하는 포트.
 * JPA 엔티티(Prompt) 저장을 담당하며, 도메인 모델과 JPA 엔티티 간 변환도 수행한다.
 */
public interface SavePromptVersionPort {

    /**
     * LLM 호출 전 사용자 존재 여부를 검증한다.
     * 탈퇴·삭제된 유저로 인한 불필요한 LLM 비용을 방지한다.
     *
     * @throws org.example.sharedprompts.global.exception.ApiException USER_NOT_FOUND
     */
    void validateUserExists(Long userId);

    /**
     * 생성 결과를 저장하고 저장된 프롬프트 ID를 반환한다.
     *
     * @param command      원본 사용자 커맨드
     * @param spec         생성에 사용된 PromptSpec
     * @param finalContent 최종 생성 콘텐츠 (Repair 완료 후)
     * @param repairCount  Repair 횟수 (내부 지표용, UX 노출 금지)
     * @param finallyPassed Verify 최종 통과 여부
     * @return 저장된 프롬프트 엔티티 ID
     */
    Long save(GeneratePromptCommand command, PromptSpec spec,
              String finalContent, int repairCount, boolean finallyPassed);
}
