package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter mapper: shape transformation only. Converts command fields into a {@link ConfirmedSemanticAxes.Builder}.
 * <p>
 * Does <b>not</b> resolve objective, outputNeeds, or taskDomain—those are derived in
 * {@link org.example.sharedprompts.domain.prompt.application.service.generate.GeneratePromptFromConfirmedAxesService}
 * via IntentDictionary and CategorySemanticProfileRegistry. The service sets those on the builder and builds.
 * </p>
 */
@Component
public class ConfirmedAxesMapper {

    /**
     * Builds a {@link ConfirmedSemanticAxes.Builder} with only command-sourced fields set.
     * Caller (service) must set objective, outputNeeds, and taskDomain before calling {@code build()}.
     */
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
        // objective, outputNeeds, taskDomain are set by the service from IntentDictionary/profile
    }
}
