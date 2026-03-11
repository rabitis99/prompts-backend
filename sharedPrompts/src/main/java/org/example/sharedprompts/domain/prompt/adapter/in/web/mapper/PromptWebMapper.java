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
 * 웹 DTO ↔ 프롬프트 유즈케이스 모델 변환 매퍼
 */
@Component
public class PromptWebMapper {

    /**
     * 검색 조건 DTO → 검색 쿼리 변환
     */
    public SearchPromptsQuery toSearchQuery(PromptSearchCondition condition,
                                            Long ownerId,
                                            Long viewerId) {
        return SearchPromptsQuery.builder()
                .page(condition.getPage())
                .size(condition.getSize())
                .sort(condition.getSort())
                .category(condition.getPromptCategory())
                .ownerId(ownerId)
                .viewerId(viewerId)
                .keyword(condition.getKeyword())
                .build();
    }

    /**
     * 페이지 조회 결과 → 페이지 응답 DTO 변환
     */
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

    /**
     * 프롬프트 요약 View → 응답 DTO 변환
     */
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

    /**
     * 프롬프트 상세 View → 응답 DTO 변환
     */
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

    /**
     * 수정 요청 DTO → 업데이트 커맨드 변환
     */
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
                null // content, promptCategory는 현재 업데이트에서 지원하지 않음
        );
    }
}