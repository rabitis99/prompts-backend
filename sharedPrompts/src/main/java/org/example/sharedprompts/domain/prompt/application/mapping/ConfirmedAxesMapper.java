package org.example.sharedprompts.domain.prompt.application.mapping;

import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ConfirmedGeneratePromptCommand → ConfirmedSemanticAxes.Builder 변환 매퍼
 */
@Component
public class ConfirmedAxesMapper {

    /**
     * command 기반 축 정보로 Builder 생성
     */
    public ConfirmedSemanticAxes.Builder fromCommand(ConfirmedGeneratePromptCommand command) {

        // 적용된 프로파일 ID 구성
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

        // objective / outputNeeds / taskDomain 은 서비스에서 설정
    }
}