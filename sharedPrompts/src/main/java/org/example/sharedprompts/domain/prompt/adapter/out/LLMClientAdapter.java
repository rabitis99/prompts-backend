package org.example.sharedprompts.domain.prompt.adapter.out;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.out.LLMClientPort;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.example.sharedprompts.domain.prompt.application.port.out.PromptSpecRendererPort;
import org.example.sharedprompts.global.google.gemini.SyncGoogleGeminiClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 클라이언트 어댑터 — 현재 Google Gemini를 사용한다.
 *
 * <p>LLMClientPort 인터페이스를 통해 도메인/애플리케이션은 실제 LLM 벤더를 알지 못한다.
 * 벤더 교체 시 이 클래스만 교체하면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMClientAdapter implements LLMClientPort {

    private final SyncGoogleGeminiClient syncGoogleGeminiClient;
    private final PromptSpecRendererPort promptSpecRenderer;

    @Override
    public String solve(PromptSpec spec) {
        String metaPrompt = promptSpecRenderer.render(spec);
        log.debug("[LLMClientAdapter] Solve 호출: objective={}, strategyBundle={}",
                spec.getObjective(), spec.getStrategyBundle().getName());

        String response = syncGoogleGeminiClient.chatSync(metaPrompt);

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("LLM 응답이 비어있습니다. objective=" + spec.getObjective());
        }
        return response;
    }

    @Override
    public String repair(String draft, PromptSpec spec,
                         List<QualityRubric.RubricItem> failedItems,
                         List<String> failureReasons) {
        String failureHints = buildFailureHints(failedItems, failureReasons);
        String repairPrompt = promptSpecRenderer.renderRepair(draft, spec, failureHints);

        log.debug("[LLMClientAdapter] Repair 호출: failedItems={}", failedItems);
        String response = syncGoogleGeminiClient.chatSync(repairPrompt);

        if (response == null || response.isBlank()) {
            log.warn("[LLMClientAdapter] Repair 응답이 비어있어 원본 초안 반환");
            return draft;
        }
        return response;
    }

    private String buildFailureHints(List<QualityRubric.RubricItem> failedItems,
                                      List<String> failureReasons) {
        List<QualityRubric.RubricItem> safeFailedItems =
                failedItems != null ? failedItems : List.of();
        List<String> safeFailureReasons =
                failureReasons != null ? failureReasons : List.of();

        StringBuilder sb = new StringBuilder();
        sb.append("다음 항목이 검증에 실패했습니다. 해당 항목만 수정하고 나머지는 유지하세요:\n");
        for (int i = 0; i < safeFailedItems.size(); i++) {
            sb.append("- [").append(safeFailedItems.get(i).getDescription()).append("]");
            if (i < safeFailureReasons.size()) {
                sb.append(": ").append(safeFailureReasons.get(i));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
