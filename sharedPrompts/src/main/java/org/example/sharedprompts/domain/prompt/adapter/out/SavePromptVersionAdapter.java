package org.example.sharedprompts.domain.prompt.adapter.out;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.out.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.repository.PromptRepository;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생성된 프롬프트를 JPA 엔티티로 저장하는 어댑터.
 *
 * <p>도메인 모델(PromptSpec)과 JPA 엔티티(Prompt) 간 변환을 담당한다.
 * repairCount 등 내부 지표는 이 레이어에서 로깅하지만 JPA 엔티티에는 포함하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavePromptVersionAdapter implements SavePromptVersionPort {

    private final UserRepository userRepository;
    private final PromptRepository promptRepository;
    private final PromptTagService promptTagService;

    @Override
    public void validateUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(timeout = 30)
    public Long save(GeneratePromptCommand command, PromptSpec spec,
                     String finalContent, int repairCount, boolean finallyPassed) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Prompt prompt = Prompt.builder()
                .title(command.title())
                .description(command.description())
                .content(finalContent)
                .isPublic(Boolean.TRUE.equals(command.isPublic()))
                .promptCategory(command.promptCategory())
                .author(user)
                .build();

        Prompt saved = promptRepository.save(prompt);

        if (command.tags() != null && !command.tags().isEmpty()) {
            promptTagService.addTags(saved, command.tags());
        }

        // 내부 지표 로깅 (UX에는 노출하지 않음)
        log.info("[SavePromptVersion] 저장 완료: promptId={}, repairCount={}, finallyPassed={}, objective={}",
                saved.getId(), repairCount, finallyPassed, spec.getObjective());

        return saved.getId();
    }
}
