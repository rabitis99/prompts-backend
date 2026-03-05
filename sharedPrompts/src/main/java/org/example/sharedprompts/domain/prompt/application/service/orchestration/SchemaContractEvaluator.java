package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 스키마/출력 계약 준수 결과를 집계하고 응답/메트릭에 반영하는 컴포넌트.
 *
 * <p>현재는 V2 파이프라인의 품질 배지(특히 FORMAT_VERIFIED) 기반으로 계약 위반 여부를 추론하지만,
 * 향후에는 Verify 단계 결과(예: {@code VerifyResult} 내 {@code formatValid/jsonValid/schemaValid} 플래그)에서
 * 직접 값을 전달받도록 점진 이관한다.</p>
 */
@Component
public class SchemaContractEvaluator {

    public SchemaContractEvaluation evaluate(GeneratePromptResult result) {
        boolean failed = hasSchemaContractFailure(result);
        List<String> reasons = failed
                ? List.of("FORMAT_COMPLIANCE (JSON Schema/OutputContract) rubric failed.")
                : List.of();
        return new SchemaContractEvaluation(failed, reasons);
    }

    private boolean hasSchemaContractFailure(GeneratePromptResult result) {
        // 품질 배지에서 형식 검증 배지(FORMAT_VERIFIED) 부재를 스키마/포맷 계약 실패로 간주한다.
        return result.badges().stream()
                .noneMatch(badge -> badge.name().equals("FORMAT_VERIFIED"));
    }

    public record SchemaContractEvaluation(
            boolean schemaContractFailed,
            List<String> schemaFailureReasons
    ) {
    }
}

