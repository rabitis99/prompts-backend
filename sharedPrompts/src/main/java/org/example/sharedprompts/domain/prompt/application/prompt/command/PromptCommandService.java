package org.example.sharedprompts.domain.prompt.application.prompt.command;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.exception.PromptAccessDeniedException;
import org.example.sharedprompts.domain.prompt.application.exception.PromptNotFoundException;
import org.example.sharedprompts.domain.prompt.application.port.in.prompt.PromptCommandUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.DeletePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UpdatePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptDeletedEvent;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptEventPort;
import org.example.sharedprompts.domain.prompt.application.port.out.like.LikeCountPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptQueryPort;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagQueryPort;
import org.example.sharedprompts.domain.prompt.application.mapping.PromptDetailViewMapper;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 프롬프트 수정/삭제 담당 */
@Service
@RequiredArgsConstructor
public class PromptCommandService implements PromptCommandUseCase {

    private final PromptQueryPort promptQueryPort;
    private final PromptCommandPort promptCommandPort;
    private final PromptTagCommandPort promptTagCommandPort;
    private final PromptTagQueryPort promptTagQueryPort;
    private final LikeCountPort likeCountPort;
    private final PromptEventPort promptEventPort;
    private final PromptDetailViewMapper promptDetailViewMapper;
    private final PromptUpdateValidator promptUpdateValidator;

    @Override
    @Transactional
    public PromptDetailView updatePrompt(UpdatePromptCommand command) {
        Long promptId = command.promptId();
        Long userId = command.userId();

        Prompt prompt = promptQueryPort.findById(promptId)
                .orElseThrow(() -> new PromptNotFoundException(promptId));

        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new PromptAccessDeniedException(promptId, userId);
        }

        UpdatePromptPayload request = new UpdatePromptPayload(
                command.title(),
                command.description(),
                command.isPublic(),
                command.tags(),
                command.content()
        );

        UpdatePromptPayload validated = promptUpdateValidator.validateAndNormalize(request);

        if (validated.title() != null) {
            prompt.updateTitle(validated.title());
        }
        if (validated.description() != null) {
            prompt.updateDescription(validated.description());
        }
        if (validated.isPublic() != null) {
            prompt.updateIsPublic(validated.isPublic());
        }
        if (validated.content() != null) {
            prompt.updateContent(validated.content());
        }

        if (validated.tags() != null) {
            promptTagCommandPort.updateTags(prompt, validated.tags());
        }

        promptCommandPort.save(prompt);

        List<String> tagNames = promptTagQueryPort.getTagNames(promptId);
        Long likeCount = likeCountPort
                .getPromptLikeCounts(List.of(promptId))
                .getOrDefault(promptId, 0L);
        return promptDetailViewMapper.toDetailView(prompt, tagNames, likeCount);
    }

    @Override
    @Transactional
    public void deletePrompt(DeletePromptCommand command) {
        Long promptId = command.promptId();
        Long userId = command.userId();

        Prompt prompt = promptQueryPort.findById(promptId)
                .orElseThrow(() -> new PromptNotFoundException(promptId));

        if (!prompt.getAuthor().getId().equals(userId)) {
            throw new PromptAccessDeniedException(promptId, userId);
        }

        Long authorId = prompt.getAuthor().getId();

        promptTagCommandPort.updateTags(prompt, List.of());
        promptCommandPort.delete(prompt);

        promptEventPort.publishPromptDeleted(new PromptDeletedEvent(promptId, authorId));
    }
}
