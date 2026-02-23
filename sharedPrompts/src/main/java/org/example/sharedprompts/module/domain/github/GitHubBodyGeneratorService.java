package org.example.sharedprompts.module.domain.github;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.module.domain.production.service.ai.text.TextAiClient;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto.Kind;
import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * GitHub Issue/PR 본문 Markdown 생성.
 * path의 promptId로 조회한 프롬프트 content를 본문 템플릿으로 사용(Issue·PR 공통). null이면 GitHubBodyTemplates 상수 사용.
 */
@Service
@Slf4j
public class GitHubBodyGeneratorService {

    private final TextAiClient textAiClient;
    private final PromptService promptService;

    public GitHubBodyGeneratorService(
            @Autowired(required = false) @Nullable TextAiClient textAiClient,
            PromptService promptService) {
        this.textAiClient = textAiClient;
        this.promptService = promptService;
    }

    public GitHubBodyPair generateBoth(Long bodyTemplatePromptId, GitHubBodyRequestDto request) {
        String issueBody = generateBody(bodyTemplatePromptId, request, Kind.ISSUE);
        String prBody = generateBody(bodyTemplatePromptId, request, Kind.PR);
        return new GitHubBodyPair(issueBody, prBody);
    }

    public String generateBody(Long bodyTemplatePromptId, GitHubBodyRequestDto request, Kind kind) {
        GitHubBodyVars vars = GitHubBodyVars.from(request);
        Map<String, String> varMap = vars.toMap();
        String template = resolveBodyTemplate(bodyTemplatePromptId, kind);

        if (textAiClient != null) {
            try {
                String systemPrompt = kind == Kind.PR ? GitHubBodyTemplates.AI_SYSTEM_PR : GitHubBodyTemplates.AI_SYSTEM_ISSUE;
                String userPrompt = buildUserPrompt(vars, kind, template);
                String combinedPrompt = systemPrompt + "\n\n---\n\n" + userPrompt;
                // TODO: Groq 호출 시 temperature 0.2~0.3 권장. 현재 TextAiClient가 temperature 미지원 시 기본값 사용.
                String generated = textAiClient.generateText(combinedPrompt, null, "markdown");
                if (generated != null && !generated.isBlank()) {
                    return GitHubBodyPlaceholderSubstitutor.substitute(generated.trim(), varMap);
                }
            } catch (Exception e) {
                log.warn("GitHub body AI generation failed, using template fallback: {}", e.getMessage());
            }
        }

        return GitHubBodyPlaceholderSubstitutor.substitute(template, varMap);
    }

    public record GitHubBodyPair(String issueBody, String prBody) {}

    /** path의 promptId가 있으면 prompts 테이블에서 조회(Issue·PR 공통 템플릿), 없으면 GitHubBodyTemplates 상수. */
    private String resolveBodyTemplate(Long bodyTemplatePromptId, Kind kind) {
        if (bodyTemplatePromptId != null) {
            try {
                String content = promptService.getPromptDetail(bodyTemplatePromptId, null).getContent();
                if (content != null && !content.isBlank()) return content;
            } catch (Exception e) {
                log.warn("Failed to load body template by promptId {}: {}", bodyTemplatePromptId, e.getMessage());
                throw new BaseException(ModuleErrorCode.GITHUB_BODY_PROMPT_NOT_FOUND, "prompts/id not found: " + bodyTemplatePromptId, e);
            }
        }
        if (bodyTemplatePromptId != null) {
            log.warn("Body template for promptId {} is blank, falling back to default template", bodyTemplatePromptId);
        }
        return kind == Kind.PR ? GitHubBodyTemplates.PR_BODY : GitHubBodyTemplates.ISSUE_BODY;
    }

    private String buildUserPrompt(GitHubBodyVars v, Kind kind, String outputStructureTemplate) {
        return """
                Generate the GitHub %s body. Use the following output structure (keep {{REPO}}, {{SHA}}, {{BRANCH}}, {{BASE_BRANCH}}, {{JOB_ID}}, {{DELIVERY_ID}} as-is). Fill every other part from the input below.

                OUTPUT STRUCTURE:
                %s

                ---

                INPUT:
                Repository: %s
                Base Branch: %s
                Branch: %s
                Commit SHA: %s
                Title: %s
                Author: %s
                Date: %s

                Commits:
                %s

                Changed Files:
                %s
                """
                .formatted(
                        kind == Kind.PR ? "Pull Request" : "Issue",
                        outputStructureTemplate,
                        v.repo(), v.baseBranch(), v.branch(), v.sha(),
                        v.title(), v.author(), v.date(),
                        v.commits(), v.files()
                );
    }
}
