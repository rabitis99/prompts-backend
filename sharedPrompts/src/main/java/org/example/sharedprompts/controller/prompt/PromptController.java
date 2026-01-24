package org.example.sharedprompts.controller.prompt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.facade.PromptFacade;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.dto.common.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final PromptFacade promptFacade;

    @PostMapping
    public ResponseEntity<CustomResponse<PromptResponseDto>> createPrompt(
            @Valid @RequestBody PromptRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        PromptResponseDto result = promptFacade.createPrompt(request, authUser.getId());
        return CustomResponseHelper.created(result);
    }

    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getPrompts(
            @ModelAttribute("condition") PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;
        PageResponse<PromptResponseDto> response = promptService.getPrompts(condition, viewerId);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptResponseDto>> getPromptDetail(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;
        return CustomResponseHelper.ok(promptService.getPromptDetail(id, viewerId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptResponseDto>> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody PromptUpdateDto promptUpdateDto,
            @CurrentUser AuthUser authUser
    ) {
        return CustomResponseHelper.ok(promptService.updatePrompt(id, promptUpdateDto, authUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(
            @PathVariable Long id,
            @CurrentUser AuthUser authUser
    ) {
        promptService.deletePrompt(id, authUser.getId());
        return CustomResponseHelper.noContent();
    }

    @GetMapping("/me")
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getMyPrompts(
            @CurrentUser AuthUser authUser,
            @ModelAttribute("condition") PromptSearchCondition condition
    ) {
        PageResponse<PromptResponseDto> response = promptService.getMyPrompts(authUser.getId(), condition);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 다른 사용자의 프롬프트 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getUserPrompts(
            @PathVariable Long userId,
            @ModelAttribute("condition") PromptSearchCondition condition,
            @CurrentUser AuthUser authUser
    ) {
        Long viewerId = authUser != null ? authUser.getId() : null;
        PageResponse<PromptResponseDto> response = promptService.getUserPrompts(userId, condition, viewerId);
        return CustomResponseHelper.ok(response);
    }
}
