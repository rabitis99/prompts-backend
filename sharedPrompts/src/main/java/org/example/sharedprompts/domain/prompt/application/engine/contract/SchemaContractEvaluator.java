package org.example.sharedprompts.domain.prompt.application.engine.contract;

import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptResult;
import org.springframework.stereotype.Component;

import java.util.List;

/** 출력 스키마(OutputContract) 준수 여부 평가 */
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