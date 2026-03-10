package org.example.sharedprompts.domain.prompt.application.mapping;

import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.springframework.stereotype.Component;

import java.util.List;

/** Command → ConfirmedSemanticAxes.Builder. objective/outputNeeds/taskDomain은 서비스에서 설정 */
@Component
public class ConfirmedAxesMapper {

    public ConfirmedSemanticAxes.Builder fromCommand(ConfirmedGeneratePromptCommand command) {
        List<String> appliedProfileIds = List.of(
                "confirmed:" + command.category().name(),
                "intent:" + command.intent().name()
        );
        return ConfirmedSemanticAxes.builder()
                .category(command.category())
                .intent(command.intent())
                .role(command.roleType())
                .actionType(command.actionType())
                .tone(command.tone())
                .style(command.style())
                .language(command.language())
                .experienceLevel(command.experience())
                .appliedProfileIds(appliedProfileIds)
                .validationWarnings(List.of())
                .recommendationHints(List.of());
    }
}