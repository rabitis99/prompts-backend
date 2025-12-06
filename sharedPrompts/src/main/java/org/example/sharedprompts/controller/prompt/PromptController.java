package org.example.sharedprompts.controller.prompt;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.dto.prompt.request.PromptRequestDto;
import org.example.sharedprompts.dto.prompt.request.PromptSearchCondition;
import org.example.sharedprompts.dto.prompt.request.PromptUpdateDto;
import org.example.sharedprompts.dto.prompt.response.PromptResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @PostMapping
    public ResponseEntity<CustomResponse<PromptResponseDto>> createPrompt(
            @RequestBody PromptRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        return CustomResponseHelper.created(promptService.createPrompt(request, authUser.getId()));
    }

    @GetMapping
    public ResponseEntity<CustomResponse<PageResponse<PromptResponseDto>>> getPrompts(
            @ModelAttribute PromptSearchCondition condition
    ) {
        PageResponse<PromptResponseDto> response = promptService.getPrompts(condition);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptResponseDto>> getPromptDetail(@PathVariable Long id) {
        return CustomResponseHelper.ok(promptService.getPromptDetail(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomResponse<PromptResponseDto>> updatePrompt(
            @PathVariable Long id,
            @RequestBody PromptUpdateDto promptUpdateDto,
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
            @ModelAttribute PromptSearchCondition condition
    ) {
        PageResponse<PromptResponseDto> response = promptService.getMyPrompts(authUser.getId(), condition);
        return CustomResponseHelper.ok(response);
    }
}
