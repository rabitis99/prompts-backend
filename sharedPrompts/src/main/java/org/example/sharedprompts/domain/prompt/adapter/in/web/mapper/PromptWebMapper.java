package org.example.sharedprompts.domain.prompt.adapter.in.web.mapper;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptDetailResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptSummaryResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UpdatePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptPageResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptSummaryView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.SearchPromptsQuery;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 웹 DTO ↔ Prompt 유즈케이스 모델 매퍼.
 */
@Component
public class PromptWebMapper {

    public SearchPromptsQuery toSearchQuery(PromptSearchCondition condition,
                                            Long ownerId,
                                            Long viewerId) {
        return new SearchPromptsQuery(
                condition.getPage(),
                condition.getSize(),
                condition.getSort(),
                condition.getPromptCategory(),
                ownerId,
                viewerId
        );
    }

    public PageResponse<PromptSummaryResponse> toPageResponse(PromptPageResult<PromptSummaryView> page) {
        List<PromptSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();

        return PageResponse.<PromptSummaryResponse>builder()
                .content(content)
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages())
                .last(page.last())
                .build();
    }

    public PromptSummaryResponse toSummaryResponse(PromptSummaryView view) {
        return new PromptSummaryResponse(
                view.id(),
                view.title(),
                view.category(),
                view.tags(),
                view.authorId(),
                view.authorNickname(),
                view.likeCount(),
                view.viewCount(),
                view.createdAt()
        );
    }

    public PromptDetailResponse toDetailResponse(PromptDetailView view) {
        return new PromptDetailResponse(
                view.id(),
                view.title(),
                view.description(),
                view.content(),
                view.category(),
                view.tags(),
                view.authorId(),
                view.authorNickname(),
                view.likeCount(),
                view.viewCount(),
                view.isPublic(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public UpdatePromptCommand toUpdateCommand(Long promptId,
                                               Long userId,
                                               PromptUpdateDto dto) {
        return new UpdatePromptCommand(
                promptId,
                userId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getIsPublic(),
                dto.getTags(),
                null // content는 PromptUpdateDto 에 없으므로 현재는 지원하지 않음
        );
    }
}

