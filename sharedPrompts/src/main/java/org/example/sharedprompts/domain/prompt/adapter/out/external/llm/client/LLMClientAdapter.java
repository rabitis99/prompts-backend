package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.ConstrainedDecodingPort;
import org.example.sharedprompts.domain.prompt.application.port.out.llm.LLMClientPort;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.application.port.out.render.PromptSpecRendererPort;
import org.example.sharedprompts.global.exception.LLMResponseException;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 호출 어댑터 (Gemini 사용)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMClientAdapter implements LLMClientPort {

    private final SyncGoogleGeminiClient syncGoogleGeminiClient;
    private final PromptSpecRendererPort promptSpecRenderer;
    private final ConstrainedDecodingPort constrainedDecodingPort;

    @Override
    public String solve(PromptSpec spec) {
        if (spec.isUseConstrainedDecoding() && spec.getOutputContract() != null && spec.getOutputContract().hasJsonSchema()) {
            String prompt = promptSpecRenderer.render(spec);
            log.debug("[LLMClientAdapter] Solve via ConstrainedDecoding: strategyBundle={}", spec.getStrategyBundle().getName());
            return constrainedDecodingPort.generateConstrained(prompt, spec.getOutputContract());
        }

        // PromptSpec → 메타 프롬프트 변환
        String metaPrompt = promptSpecRenderer.render(spec);

        log.debug("[LLMClientAdapter] Solve 호출: strategyBundle={}",
                spec.getStrategyBundle().getName());

        // LLM 호출
        String response = syncGoogleGeminiClient.chatSync(metaPrompt);

        // 응답 유효성 확인
        if (response == null || response.isBlank()) {
            throw new LLMResponseException("LLM 응답이 비어있습니다.");
        }

        return response;
    }

    @Override
    public String repair(String draft, PromptSpec spec,
                         List<QualityRubric.RubricItem> failedItems,
                         List<String> failureReasons) {

        if (failedItems == null || failedItems.isEmpty()) {
            log.debug("[LLMClientAdapter] Repair 스킵: failedItems 없음");
            return draft;
        }

        // 실패 항목 설명 생성
        String failureHints = buildFailureHints(failedItems, failureReasons);

        // 수정 프롬프트 생성
        String repairPrompt = promptSpecRenderer.renderRepair(draft, spec, failureHints);

        log.debug("[LLMClientAdapter] Repair 호출: failedItems={}", failedItems);

        // LLM 재호출
        String response = syncGoogleGeminiClient.chatSync(repairPrompt);

        // 빈 응답을 draft로 삼키면 호출자 루프가 같은 초안으로 재시도만 반복하므로 예외로 전파
        if (response == null || response.isBlank()) {
            log.warn("[LLMClientAdapter] Repair 응답이 비어있음");
            throw new LLMResponseException("LLM Repair 응답이 비어있습니다.");
        }

        return response;
    }

    /**
     * 검증 실패 항목을 LLM 수정 힌트로 변환
     */
    private String buildFailureHints(List<QualityRubric.RubricItem> failedItems,
                                     List<String> failureReasons) {

        List<QualityRubric.RubricItem> safeFailedItems =
                failedItems != null ? failedItems : List.of();

        List<String> safeFailureReasons =
                failureReasons != null ? failureReasons : List.of();

        if (safeFailedItems.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 실패 항목 안내 문구
        sb.append("다음 항목이 검증에 실패했습니다. 해당 항목만 수정하고 나머지는 유지하세요:\n");

        for (int i = 0; i < safeFailedItems.size(); i++) {

            // 실패한 Rubric 항목
            sb.append("- [").append(safeFailedItems.get(i).getDescription()).append("]");

            if (i < safeFailureReasons.size()) {
                sb.append(": ").append(safeFailureReasons.get(i));
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}