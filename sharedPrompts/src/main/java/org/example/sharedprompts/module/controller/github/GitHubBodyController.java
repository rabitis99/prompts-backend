package org.example.sharedprompts.module.controller.github;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.github.GitHubBodyGenerateApplicationService;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub Issue/PR 본문 생성 API (Markdown만 생성·저장·반환).
 * 오케스트레이션은 GitHubBodyGenerateApplicationService에 위임.
 */
@RestController
@RequestMapping("prompts/{promptId}/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubBodyController {

    private final GitHubBodyGenerateApplicationService applicationService;

    @PostMapping("/bodies/generate")
    public ResponseEntity<CustomResponse<GitHubBodyResponseDto>> generate(
            @PathVariable Long promptId,
            @Valid @RequestBody GitHubBodyRequestDto request
    ) {
        GitHubBodyResponseDto response = applicationService.generate(promptId, request);
        return CustomResponseHelper.ok(response);
    }
}
