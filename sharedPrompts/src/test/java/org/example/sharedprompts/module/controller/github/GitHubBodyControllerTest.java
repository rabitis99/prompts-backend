package org.example.sharedprompts.module.controller.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.module.domain.github.GitHubBodyGenerateApplicationService;
import org.example.sharedprompts.module.domain.github.GitHubBodyGeneratorService;
import org.example.sharedprompts.module.domain.github.GitHubBodyStorageService;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.example.sharedprompts.module.dto.response.github.GitHubBodyResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubBodyController.class)
@DisplayName("GitHubBodyController 통합 테스트")
class GitHubBodyControllerTest {

    private static final Long PROMPT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubBodyGenerateApplicationService applicationService;
    @MockBean
    private GitHubBodyGeneratorService generatorService;
    @MockBean
    private GitHubBodyStorageService storageService;

    @Test
    @DisplayName("POST /prompts/{promptId}/github/bodies/generate - jobId 있으면 생성 후 200 + body + keys")
    void generate_returnsBodiesAndKeys() throws Exception {
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "job-1", null, null, "owner/repo", "main", null,
                "title", null, null, "commits", "files", null);
        var response = new GitHubBodyResponseDto("job-1", "# Issue", "# PR", "s3/issue.md", "s3/pr.md");
        when(applicationService.generate(eq(PROMPT_ID), any())).thenReturn(response);

        mockMvc.perform(post("/prompts/{promptId}/github/bodies/generate", PROMPT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-1"))
                .andExpect(jsonPath("$.data.issueBody").value("# Issue"))
                .andExpect(jsonPath("$.data.prBody").value("# PR"))
                .andExpect(jsonPath("$.data.storedIssueFileKey").value("s3/issue.md"))
                .andExpect(jsonPath("$.data.storedPrFileKey").value("s3/pr.md"));

        verify(applicationService).generate(eq(PROMPT_ID), any());
    }

    @Test
    @DisplayName("jobId/deliveryId/sha 모두 없으면 400")
    void generate_missingJobId_returns400() throws Exception {
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/prompts/{promptId}/github/bodies/generate", PROMPT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
