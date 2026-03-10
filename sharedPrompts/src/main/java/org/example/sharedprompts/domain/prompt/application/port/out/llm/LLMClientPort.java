package org.example.sharedprompts.domain.prompt.application.port.out.llm;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;

import java.util.List;

/** LLM 호출 아웃바운드 포트 (Solve/Repair) */
public interface LLMClientPort {

    String solve(PromptSpec spec);

    String repair(String draft, PromptSpec spec,
                  List<QualityRubric.RubricItem> failedItems,
                  List<String> failureReasons);
}
