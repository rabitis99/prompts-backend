package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 스키마/출력 계약 준수 결과를 집계하고 응답/메트릭에 반영하는 컴포넌트.
 *
 * <p>Verify 단계의 FORMAT_COMPLIANCE 루브릭 결과가 {@link GeneratePromptResult#formatValid()}로
 * 전달되므로, 배지 추론 없이 직접 플래그를 사용해 계약 실패 여부를 판단한다.</p>
 */
@Component
public class SchemaContractEvaluator {

    public SchemaContractEvaluation evaluate(GeneratePromptResult result) {
        if (result == null) {
            return new SchemaContractEvaluation(true, List.of("GeneratePromptResult is null"));
        }
        boolean failed = !result.formatValid();
        List<String> reasons = failed
                ? List.of("FORMAT_COMPLIANCE (JSON Schema/OutputContract) rubric failed.")
                : List.of();
        return new SchemaContractEvaluation(failed, reasons);
    }

    public record SchemaContractEvaluation(
            boolean schemaContractFailed,
            List<String> schemaFailureReasons
    ) {
    }
}

