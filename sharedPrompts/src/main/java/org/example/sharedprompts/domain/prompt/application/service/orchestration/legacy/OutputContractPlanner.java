package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON Schema 유무 및 OutputNeeds/Objective 정합성을 기준으로
 * 출력 계약(Contract)을 계획/강제하는 컴포넌트.
 */
@Component
public class OutputContractPlanner {

    public ContractDecision plan(
            UnifiedGeneratePromptCommand command,
            PromptObjective currentObjective,
            OutputNeeds currentOutputNeeds
    ) {
        List<String> reasons = new ArrayList<>();
        boolean hasSchema = hasJsonSchema(command);

        PromptObjective objective = currentObjective;
        OutputNeeds outputNeeds = currentOutputNeeds;
        boolean schemaRequired = false;

        if (hasSchema) {
            objective = PromptObjective.EXTRACTION;
            outputNeeds = OutputNeeds.JSON_SCHEMA_REQUIRED;
            schemaRequired = true;
            reasons.add("jsonSchema:forceExtraction");
            reasons.add("jsonSchema:forceJsonSchemaRequired");
        }

        return new ContractDecision(objective, outputNeeds, schemaRequired, List.copyOf(reasons));
    }

    private boolean hasJsonSchema(UnifiedGeneratePromptCommand command) {
        return command.jsonSchema() != null && !command.jsonSchema().isBlank();
    }
}

