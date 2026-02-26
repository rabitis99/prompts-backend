package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;

/**
 * 생성된 프롬프트를 저장하는 포트.
 * JPA 엔티티(Prompt) 저장을 담당하며, 도메인 모델과 JPA 엔티티 간 변환도 수행한다.
 */
public interface SavePromptVersionPort {

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
