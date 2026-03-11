package org.example.sharedprompts.domain.prompt.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptDetailResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptSummaryResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.mapper.PromptWebMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.command.DeletePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.prompt.PromptCommandUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.prompt.PromptQueryUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptPageResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptSummaryView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.SearchPromptsQuery;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 프롬프트 조회/수정/삭제 API 컨트롤러
 * 생성 기능은 PromptGenerationController 에서 담당한다.
 */
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptQueryUseCase promptQueryUseCase;
    private final PromptCommandUseCase promptCommandUseCase;
    private final PromptWebMapper promptWebMapper;

    /** 전체 프롬프트 목록 조회 */
    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<PromptSummaryResponse>>> getPrompts(
            @Valid PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;

        SearchPromptsQuery query = promptWebMapper.toSearchQuery(condition, null, viewerId);
        PromptPageResult<PromptSummaryView> result = promptQueryUseCase.getPrompts(query);
        PageResponse<PromptSummaryResponse> response = promptWebMapper.toPageResponse(result);

        return CustomResponseHelper.ok(response);
    }

    /** 내 프롬프트 목록 조회 */
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<PageResponse<PromptSummaryResponse>>> getMyPrompts(
            @Valid PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long userId = authUser.getId();

        SearchPromptsQuery query = promptWebMapper.toSearchQuery(condition, userId, null);
        PromptPageResult<PromptSummaryView> result = promptQueryUseCase.getMyPrompts(query);
        PageResponse<PromptSummaryResponse> response = promptWebMapper.toPageResponse(result);

        return CustomResponseHelper.ok(response);
    }

    /** 특정 사용자의 프롬프트 목록 조회 */
    @GetMapping("/users/{userId}")
    public ResponseEntity<CustomResponse<PageResponse<PromptSummaryResponse>>> getUserPrompts(
            @PathVariable Long userId,
            @Valid PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;

        SearchPromptsQuery query = promptWebMapper.toSearchQuery(condition, userId, viewerId);
        PromptPageResult<PromptSummaryView> result = promptQueryUseCase.getUserPrompts(query);
        PageResponse<PromptSummaryResponse> response = promptWebMapper.toPageResponse(result);

        return CustomResponseHelper.ok(response);
    }

    /** 프롬프트 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptDetailResponse>> getPromptDetail(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;

        var view = promptQueryUseCase.getPromptDetail(id, viewerId);
        PromptDetailResponse response = promptWebMapper.toDetailResponse(view);

        return CustomResponseHelper.ok(response);
    }

    /** 프롬프트 수정 */
    @PatchMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptDetailResponse>> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody PromptUpdateDto request,
            @CurrentUser AuthUser authUser
    ) {
        Long userId = authUser.getId();

        var command = promptWebMapper.toUpdateCommand(id, userId, request);
        var view = promptCommandUseCase.updatePrompt(command);
        PromptDetailResponse response = promptWebMapper.toDetailResponse(view);

        return CustomResponseHelper.ok(response);
    }

    /** 프롬프트 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        Long userId = authUser.getId();

        promptCommandUseCase.deletePrompt(new DeletePromptCommand(id, userId));

        return CustomResponseHelper.noContent();
    }
}

