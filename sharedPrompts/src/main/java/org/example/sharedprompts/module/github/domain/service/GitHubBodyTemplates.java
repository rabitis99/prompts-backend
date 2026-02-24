package org.example.sharedprompts.module.github.domain.service;

/**
 * Issue/PR 본문 템플릿 및 AI 시스템 프롬프트 상수.
 * 변경 시 이 클래스만 수정하면 되도록 분리.
 */
public final class GitHubBodyTemplates {

    private GitHubBodyTemplates() {}

    public static final String ISSUE_BODY = """
            ## TL;DR
            - TODO: Summarize technical intent from commit messages.
            - TODO: Summarize impact (scope, affected areas).
            - TODO: Follow-up or dependency if any.

            ## Background / Context
            Changes in this push (branch **{{BRANCH}}**) are described by the commit messages below.

            ```
            {{COMMITS}}
            ```

            If the above is empty or unclear, add a short explanation of what changed and why.

            ## Requirements
            - [ ] TODO: Derive from commit intent (e.g. behaviour implemented, validation added).
            - [ ] TODO: Add further items based on commits and changed files.

            ## Acceptance Criteria
            - [ ] TODO: Define measurable success (e.g. tests pass, no new linter errors).
            - [ ] TODO: Add criteria matching scope of changed files and commit intent.

            ## Risks & Notes
            TODO: Risk analysis needed. After reviewing commits and files, consider: breaking changes, DB/schema impact, API contract, performance, concurrency, deployment risk.

            ## Test Plan
            - **Unit tests:** TODO: Suggest tests for new or changed logic.
            - **Integration tests:** TODO: Suggest API or DB integration tests if applicable.
            - **Manual verification:** TODO: Steps to validate in dev/staging.

            ## Links
            - Commit: https://github.com/{{REPO}}/commit/{{SHA}}

            ---
            [AUTO_JOB_ID:{{JOB_ID}}]
            [AUTO_PUSH_DELIVERY:{{DELIVERY_ID}}]
            [AUTO_PUSH_SHA:{{SHA}}]
            """;

    public static final String PR_BODY = """
            # Summary
            - TODO: Summarize main scope from commit messages.
            - TODO: State primary intent (e.g. feature, fix, refactor).
            - TODO: Note any notable side effects or follow-ups.

            # What Changed
            ## Key Modifications
            - TODO: Group changes by area (e.g. API, domain, persistence) using commits and files.
            - TODO: Call out structural or architectural impact if any.

            ## Files Impacted
            - TODO: Classify touched files (controller / service / domain / config / tests). If unclear, omit or list paths only.

            # Why
            TODO: Clarify motivation from commit messages. If not inferable, leave as "TODO: clarify motivation".

            # Risk Analysis
            - **Breaking changes:** TODO: Yes/No + brief note.
            - **Backward compatibility:** TODO: Assess (API, config, behaviour).
            - **DB or data impact:** TODO: None / migration / data change.
            - **Performance impact:** TODO: None / localized / broader.
            - **Concurrency issues:** TODO: None / possible / confirmed.
            - **Deployment risk:** TODO: Low / Medium / High and why.

            **Risk level:** TODO: LOW / MEDIUM / HIGH
            **Reasoning:** TODO: One or two sentences.

            # How to Test
            - **Unit tests:** TODO: What to run or add.
            - **Integration tests:** TODO: Scenarios or suites to run.
            - **Manual verification:** TODO: Steps (e.g. call endpoint, check DB/UI).

            # Rollback Plan
            TODO: Define rollback strategy (e.g. revert commit, feature flag off, DB rollback). If unclear: "TODO: define rollback strategy".

            # Checklist
            - [ ] Code reviewed
            - [ ] Tests updated
            - [ ] Backward compatibility checked
            - [ ] Rollback considered

            # Related Links
            - Compare: https://github.com/{{REPO}}/compare/{{BASE_BRANCH}}...{{BRANCH}}
            - Commit: https://github.com/{{REPO}}/commit/{{SHA}}

            ---
            [AUTO_JOB_ID:{{JOB_ID}}]
            [AUTO_PUSH_DELIVERY:{{DELIVERY_ID}}]
            [AUTO_PUSH_SHA:{{SHA}}]
            """;

    public static final String AI_SYSTEM_ISSUE = """
            You are a senior software engineer writing a professional GitHub Issue body in Markdown.
            RULES: Output Markdown only. Use ONLY the provided commit and file information; do not hallucinate.
            If something is unclear, write "TODO:" instead of guessing. Keep the exact section structure.
            In the output, keep the placeholders {{REPO}}, {{SHA}}, {{BRANCH}}, {{JOB_ID}}, {{DELIVERY_ID}} exactly as written (they will be replaced by the system).
            Fill all other sections (TL;DR, Background, Requirements, Acceptance Criteria, Risks, Test Plan) from the commits and changed files.
            """;

    public static final String AI_SYSTEM_PR = """
            You are a senior backend engineer writing a professional GitHub Pull Request body in Markdown.
            RULES: Output Markdown only. Use ONLY the provided commit and file information; do not hallucinate.
            If something is unclear, write "TODO:". Keep the exact section structure.
            In the output, keep the placeholders {{REPO}}, {{SHA}}, {{BRANCH}}, {{BASE_BRANCH}}, {{JOB_ID}}, {{DELIVERY_ID}} exactly as written (they will be replaced by the system).
            Fill all sections (Summary, What Changed, Why, Risk Analysis, How to Test, Rollback Plan) from the commits and changed files.
            """;
}
