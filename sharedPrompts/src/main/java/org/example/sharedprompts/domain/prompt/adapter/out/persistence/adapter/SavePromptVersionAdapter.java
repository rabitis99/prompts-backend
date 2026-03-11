package org.example.sharedprompts.domain.prompt.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.application.prompt.notification.PromptNotificationService;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 생성된 프롬프트 저장 어댑터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavePromptVersionAdapter implements SavePromptVersionPort {

    private final UserRepository userRepository;
    private final PromptCommandPort promptCommandPort;
    private final PromptTagService promptTagService;
    private final PromptNotificationService promptNotificationService;

    @Override
    @Transactional(timeout = 30)
    public Long save(GeneratePromptCommand command, @NotNull PromptSpec spec,
                     String finalContent, int repairCount, boolean finallyPassed) {
        Objects.requireNonNull(spec, "spec must not be null");

        // 사용자 조회
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // description 기본값 처리
        String description = (command.description() != null && !command.description().isBlank())
                ? command.description()
                : "";

        // title 기본값 처리
        String title = (command.title() != null && !command.title().isBlank())
                ? command.title()
                : "Untitled";

        // Prompt 엔티티 생성
        Prompt prompt = Prompt.builder()
                .title(title)
                .description(description)
                .content(finalContent)
                .isPublic(Boolean.TRUE.equals(command.isPublic()))
                .promptCategory(command.promptCategory())
                .author(user)
                .build();

        // 프롬프트 저장
        Prompt saved = promptCommandPort.save(prompt);

        // 태그 저장
        promptTagService.addTags(saved, command.tags());

        // 프롬프트 생성 알림 발행 (커밋 후 SSE 등)
        promptNotificationService.publishPromptCreated(saved, user);

        // 내부 저장 로그 (objective는 enum 이름만 기록, 사용자 원문 미포함)
        log.info("[SavePromptVersion] 저장 완료: promptId={}, repairCount={}, finallyPassed={}, objective={}",
                saved.getId(), repairCount, finallyPassed,
                spec.getObjective() == null ? null : spec.getObjective().name());

        return saved.getId();
    }
}