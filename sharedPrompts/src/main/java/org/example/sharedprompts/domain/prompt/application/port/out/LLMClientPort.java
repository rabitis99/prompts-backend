package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.model.VerifyResult;

import java.util.List;

/**
 * LLM 호출 포트 — 도메인/애플리케이션이 의존하는 추상화 경계.
 * 실제 LLM 벤더(Gemini, OpenAI 등)는 어댑터에서만 안다.
 */
public interface LLMClientPort {

    /**
     * PromptSpec을 기반으로 초안(draft)을 생성한다 (Solve 단계).
     *
     * @param spec 프롬프트 명세
     * @return LLM이 생성한 초안 텍스트
     */
    String solve(PromptSpec spec);

    /**
     * Repair 단계 — 실패 항목만 지목하여 수정 요청한다.
     *
     * @param draft          이전 초안
     * @param spec           프롬프트 명세
     * @param failedItems    실패한 루브릭 항목 목록
     * @param failureReasons 실패 이유 목록
     * @return 수정된 초안
     */
    String repair(String draft, PromptSpec spec,
                  List<QualityRubric.RubricItem> failedItems,
                  List<String> failureReasons);
}
