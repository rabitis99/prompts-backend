package org.example.sharedprompts.domain.prompt.application.prompt.query;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.exception.PromptAccessDeniedException;
import org.example.sharedprompts.domain.prompt.application.exception.PromptNotFoundException;
import org.example.sharedprompts.domain.prompt.application.port.in.prompt.PromptQueryUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptPageResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptSummaryView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.SearchPromptsQuery;
import org.example.sharedprompts.domain.prompt.application.port.out.block.BlockPolicyPort;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptEventPort;
import org.example.sharedprompts.domain.prompt.application.port.out.event.PromptViewedEvent;
import org.example.sharedprompts.domain.prompt.application.port.out.like.LikeCountPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptQueryPort;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.PromptSearchQuery;
import org.example.sharedprompts.domain.prompt.application.port.out.tag.PromptTagQueryPort;
import org.example.sharedprompts.domain.prompt.application.mapping.PromptDetailViewMapper;
import org.example.sharedprompts.domain.prompt.application.readmodel.PromptReadModelAssembler;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

/** 프롬프트 조회 담당 */
@Service
@RequiredArgsConstructor
public class PromptQueryService implements PromptQueryUseCase {

    private final PromptQueryPort promptQueryPort;
    private final BlockPolicyPort blockPolicyPort;
    private final PromptTagQueryPort promptTagQueryPort;
    private final LikeCountPort likeCountPort;
    private final PromptEventPort promptEventPort;
    private final PromptDetailViewMapper promptDetailViewMapper;
    private final PromptReadModelAssembler promptReadModelAssembler;

    private PromptPageResult<PromptSummaryView> searchPrompts(SearchPromptsQuery query) {
        Page<Prompt> page = promptQueryPort.search(
                new PromptSearchQuery(
                        query.page(),
                        query.size(),
                        query.sort(),
                        query.category(),
                        query.ownerId(),
                        query.viewerId(),
                        query.keyword()
                )
        );
        return mapToPromptSummaryPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptPageResult<PromptSummaryView> getPrompts(SearchPromptsQuery query) {
        return searchPrompts(query);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptDetailView getPromptDetail(Long promptId, Long viewerId) {
        Prompt prompt = promptQueryPort.findById(promptId)
                .orElseThrow(() -> new PromptNotFoundException(promptId));

        Long authorId = prompt.getAuthor().getId();
        if (!prompt.isPublic() && (!authorId.equals(viewerId))) {
            throw new PromptAccessDeniedException(promptId, viewerId);
        }

        if (viewerId != null &&
                blockPolicyPort.isBlocked(viewerId, authorId)) {
            throw new PromptAccessDeniedException(promptId, viewerId);
        }

        promptEventPort.publishPromptViewed(new PromptViewedEvent(promptId, viewerId));

        List<String> tagNames = promptTagQueryPort.getTagNames(promptId);
        Long likeCount = likeCountPort
                .getPromptLikeCounts(List.of(promptId))
                .getOrDefault(promptId, 0L);
        return promptDetailViewMapper.toDetailView(prompt, tagNames, likeCount);
    }

    /** 내 프롬프트 목록. 호출부에서 query에 ownerId=caller, viewerId=caller 등으로 세팅해 전달한다. */
    @Override
    @Transactional(readOnly = true)
    public PromptPageResult<PromptSummaryView> getMyPrompts(SearchPromptsQuery query) {
        return searchPrompts(query);
    }

    /** 특정 사용자 프롬프트 목록. 호출부에서 query에 ownerId=대상사용자, viewerId=caller 등으로 세팅해 전달한다. 구분은 SearchPromptsQuery의 ownerId/viewerId로만 이루어진다. */
    @Override
    @Transactional(readOnly = true)
    public PromptPageResult<PromptSummaryView> getUserPrompts(SearchPromptsQuery query) {
        return searchPrompts(query);
    }

    private PromptPageResult<PromptSummaryView> mapToPromptSummaryPage(Page<Prompt> page) {
        List<Prompt> content = page.getContent();
        if (content.isEmpty()) {
            return new PromptPageResult<>(
                    List.of(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isLast()
            );
        }
        var assembled = promptReadModelAssembler.assemble(content);
        // createdAt은 UTC로 저장된다고 가정. 서버/DB가 다른 타임존을 쓰면 시스템 타임존 정책 확인 필요.
        var mappedContent = content.stream()
                .map(p -> new PromptSummaryView(
                        p.getId(),
                        p.getTitle(),
                        p.getPromptCategory(),
                        assembled.tagNamesByPromptId().getOrDefault(p.getId(), List.of()),
                        p.getAuthor().getId(),
                        p.getAuthor().getNickname(),
                        assembled.likeCountByPromptId().getOrDefault(p.getId(), 0L),
                        p.getViewCount(),
                        p.getCreatedAt().toInstant(ZoneOffset.UTC)
                ))
                .toList();

        return new PromptPageResult<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
