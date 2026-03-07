package org.example.sharedprompts.domain.prompt.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생성된 프롬프트를 JPA 엔티티로 저장하는 어댑터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavePromptVersionAdapter implements SavePromptVersionPort {

    private final UserRepository userRepository;
    private final PromptCommandPort promptCommandPort;
    private final PromptTagService promptTagService;

    @Override
    @Transactional(timeout = 30)
    public Long save(GeneratePromptCommand command, PromptSpec spec,
                     String finalContent, int repairCount, boolean finallyPassed) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // DB NOT NULL: description이 없으면 생성된 본문으로 채움 (Unified Simple 요청 대응)
        String description = (command.description() != null && !command.description().isBlank())
                ? command.description()
                : (finalContent != null && !finalContent.isBlank() ? finalContent : "");
        String title = (command.title() != null && !command.title().isBlank())
                ? command.title()
                : "Untitled";

        Prompt prompt = Prompt.builder()
                .title(title)
                .description(description)
                .content(finalContent)
                .isPublic(Boolean.TRUE.equals(command.isPublic()))
                .promptCategory(command.promptCategory())
                .author(user)
                .build();

        Prompt saved = promptCommandPort.save(prompt);

        if (command.tags() != null && !command.tags().isEmpty()) {
            promptTagService.addTags(saved, command.tags());
        }

        // 내부 지표 로깅 (UX에는 노출하지 않음)
        log.info("[SavePromptVersion] 저장 완료: promptId={}, repairCount={}, finallyPassed={}, objective={}",
                saved.getId(), repairCount, finallyPassed, spec.getObjective());

        return saved.getId();
    }
}
