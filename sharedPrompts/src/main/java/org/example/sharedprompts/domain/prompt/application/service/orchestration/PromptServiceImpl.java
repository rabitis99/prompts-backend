package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.policy.FollowBlockPolicy;
import org.example.sharedprompts.domain.like.service.LikeCountService;
import org.example.sharedprompts.domain.prompt.application.exception.PromptAccessDeniedException;
import org.example.sharedprompts.domain.prompt.application.exception.PromptNotFoundException;
import org.example.sharedprompts.domain.prompt.application.port.in.command.DeletePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.PromptCommandUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UpdatePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptPageResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptQueryUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptSummaryView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.SearchPromptsQuery;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptCommandPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptQueryPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptSearchQuery;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.event.PromptEventPublisher;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.tag.service.PromptTagService;
import org.springframework.data.domain.Page;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프롬프트 조회 / 수정 / 삭제를 담당하는 도메인 서비스 구현체입니다.
 * <p>
 * 프롬프트 생성은 {@link org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase} 및
 * {@link org.example.sharedprompts.domain.prompt.application.port.in.GenerateUnifiedPromptUseCase}가 담당하며,
 * 이 구현체는 생성 이외의 CRUD 책임만 가집니다.
 */
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptQueryUseCase, PromptCommandUseCase {

    private final PromptQueryPort promptQueryPort;
    private final PromptCommandPort promptCommandPort;
    private final FollowBlockPolicy followBlockPolicy;
    private final PromptTagService promptTagService;
    private final LikeCountService likeCountService;
    private final PromptEventPublisher promptEventPublisher;
    private final PromptUpdateValidator promptUpdateValidator;

    @Override
    @Transactional(readOnly = true)
    public PromptPageResult<PromptSummaryView> getPrompts(SearchPromptsQuery query) {
        Page<Prompt> page = promptQueryPort.search(
                new PromptSearchQuery(
                        query.page(),
                        query.size(),
                        query.sort(),
                        query.category(),
                        query.ownerId(),
                        query.viewerId()
                )
        );
        return mapToPromptSummaryPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptDetailView getPromptDetail(Long promptId, Long viewerId) {
        Prompt prompt = promptQueryPort.findById(promptId)
                .orElseThrow(() -> new PromptNotFoundException(promptId));

        // viewer와 author 간 BLOCKED 관계가 존재하면 접근 차단
        if (viewerId != null &&
                followBlockPolicy.isBlocked(viewerId, prompt.getAuthor().getId())) {
            throw new PromptAccessDeniedException(promptId, viewerId);
        }

        // 조회 이벤트 발행 (AFTER_COMMIT 단계에서 Redis에 조회수 증가)
        // 조회 API는 readOnly 트랜잭션을 유지하며, 조회 직후 DB UPDATE는 발생하지 않음
        promptEventPublisher.publishPromptViewed(promptId, viewerId);

        List<Tag> tags = promptTagService.getTags(prompt);
        Long likeCount = likeCountService
                .getPromptLikeCounts(List.of(promptId))
                .getOrDefault(promptId, prompt.getLikeCount());
        return new PromptDetailView(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getDescription(),
                null,
                prompt.getPromptCategory(),
                tags.stream().map(Tag::getName).toList(),
                prompt.getAuthor().getId(),
                prompt.getAuthor().getNickname(),
                likeCount,
                prompt.getViewCount(),
                prompt.isPublic(),
                prompt.getCreatedAt().toInstant(ZoneOffset.UTC),
                prompt.getUpdatedAt().toInstant(ZoneOffset.UTC)
        );
    }

    // ============ 수정 ===============
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
                null,
                command.tags()
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
        if (validated.promptCategory() != null) {
            prompt.updateCategory(validated.promptCategory());
        }

        if (validated.tags() != null) {
            promptTagService.updateTags(prompt, validated.tags());
        }

        promptCommandPort.save(prompt);

        List<Tag> tags = promptTagService.getTags(prompt);
        return new PromptDetailView(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getDescription(),
                null,
                prompt.getPromptCategory(),
                tags.stream().map(Tag::getName).toList(),
                prompt.getAuthor().getId(),
                prompt.getAuthor().getNickname(),
                prompt.getLikeCount(),
                prompt.getViewCount(),
                prompt.isPublic(),
                prompt.getCreatedAt().toInstant(ZoneOffset.UTC),
                prompt.getUpdatedAt().toInstant(ZoneOffset.UTC)
        );
    }

    // ============ 삭제 ===============
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

        promptTagService.updateTags(prompt, List.of());
        promptCommandPort.delete(prompt);

        // 통계 캐시 무효화를 위한 이벤트 발행 (트랜잭션 내부에서 발행, 커밋 후 처리됨)
        promptEventPublisher.publishPromptDeleted(promptId, authorId);
    }

    // ============ 내 프롬프트 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PromptPageResult<PromptSummaryView> getMyPrompts(SearchPromptsQuery query) {
        Page<Prompt> page = promptQueryPort.search(
                new PromptSearchQuery(
                        query.page(),
                        query.size(),
                        query.sort(),
                        query.category(),
                        query.ownerId(),
                        query.viewerId()
                )
        );
        return mapToPromptSummaryPage(page);
    }

    // ============ 다른 사용자의 프롬프트 조회 ===============
    @Override
    @Transactional(readOnly = true)
    public PromptPageResult<PromptSummaryView> getUserPrompts(SearchPromptsQuery query) {
        Page<Prompt> page = promptQueryPort.search(
                new PromptSearchQuery(
                        query.page(),
                        query.size(),
                        query.sort(),
                        query.category(),
                        query.ownerId(),
                        query.viewerId()
                )
        );
        return mapToPromptSummaryPage(page);
    }

    private PromptPageResult<PromptSummaryView> mapToPromptSummaryPage(Page<Prompt> page) {
        var content = page.map(
                p -> new PromptSummaryView(
                        p.getId(),
                        p.getTitle(),
                        p.getPromptCategory(),
                        p.getPromptTags().stream()
                                .map(tagRel -> tagRel.getTag().getName())
                                .toList(),
                        p.getAuthor().getId(),
                        p.getAuthor().getNickname(),
                        p.getLikeCount(),
                        p.getViewCount(),
                        p.getCreatedAt().toInstant(ZoneOffset.UTC)
                )
        );

        return new PromptPageResult<>(
                content.getContent(),
                content.getNumber(),
                content.getSize(),
                content.getTotalElements(),
                content.getTotalPages(),
                content.isLast()
        );
    }

}
