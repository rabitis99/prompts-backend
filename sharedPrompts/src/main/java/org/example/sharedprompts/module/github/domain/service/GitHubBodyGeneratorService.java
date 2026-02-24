package org.example.sharedprompts.module.github.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.github.dto.body.request.GitHubBodyRequestDto;
import org.example.sharedprompts.module.github.dto.body.request.GitHubBodyRequestDto.Kind;
import org.example.sharedprompts.module.github.port.out.BodyGenerationPort;
import org.example.sharedprompts.module.github.port.out.BodyTemplatePort;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * GitHub Issue/PR 본문 Markdown 생성.
 * path의 promptId로 조회한 프롬프트 content를 본문 템플릿으로 사용(Issue·PR 공통). null이면 GitHubBodyTemplates 상수 사용.
 */
@Service
@Slf4j
public class GitHubBodyGeneratorService {

    private final BodyTemplatePort bodyTemplatePort;
    private final BodyGenerationPort bodyGenerationPort;

    public GitHubBodyGeneratorService(
            BodyTemplatePort bodyTemplatePort,
            BodyGenerationPort bodyGenerationPort) {
        this.bodyTemplatePort = bodyTemplatePort;
        this.bodyGenerationPort = bodyGenerationPort;
    }

    public GitHubBodyPair generateBoth(Long bodyTemplatePromptId, GitHubBodyRequestDto request) {
        GitHubBodyVars vars = GitHubBodyVars.from(request);
        Map<String, String> varMap = vars.toMap();
        String issueTemplate = resolveBodyTemplate(bodyTemplatePromptId, Kind.ISSUE);
        String prTemplate = bodyTemplatePromptId == null ? resolveBodyTemplate(null, Kind.PR) : issueTemplate;
        String issueBody = generateBodyFromVars(vars, varMap, issueTemplate, Kind.ISSUE);
        String prBody = generateBodyFromVars(vars, varMap, prTemplate, Kind.PR);
        return new GitHubBodyPair(issueBody, prBody);
    }

    public String generateBody(Long bodyTemplatePromptId, GitHubBodyRequestDto request, Kind kind) {
        GitHubBodyVars vars = GitHubBodyVars.from(request);
        Map<String, String> varMap = vars.toMap();
        String template = resolveBodyTemplate(bodyTemplatePromptId, kind);
        return generateBodyFromVars(vars, varMap, template, kind);
    }

    private String generateBodyFromVars(GitHubBodyVars vars, Map<String, String> varMap, String template, Kind kind) {
        String systemPrompt = kind == Kind.PR ? GitHubBodyTemplates.AI_SYSTEM_PR : GitHubBodyTemplates.AI_SYSTEM_ISSUE;
        String userPrompt = buildUserPrompt(vars, kind, template);

        Optional<String> generated = bodyGenerationPort.generateBody(systemPrompt, userPrompt);
        if (generated.isPresent()) {
            return GitHubBodyPlaceholderSubstitutor.substitute(generated.get().trim(), varMap);
        }

        log.debug("AI generation skipped or failed, using template fallback for kind: {}", kind);
        return GitHubBodyPlaceholderSubstitutor.substitute(template, varMap);
    }

    public record GitHubBodyPair(String issueBody, String prBody) {}

    /** path의 promptId가 있으면 템플릿 포트에서 조회(Issue·PR 구분), 없으면 포트 기본값 사용. */
    private String resolveBodyTemplate(Long bodyTemplatePromptId, Kind kind) {
        BodyTemplatePort.TemplateKind templateKind =
                (kind == Kind.PR) ? BodyTemplatePort.TemplateKind.PR : BodyTemplatePort.TemplateKind.ISSUE;
        return bodyTemplatePort.resolveTemplate(bodyTemplatePromptId, templateKind);
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
