package org.example.sharedprompts.github.adapter.out.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.service.PromptService;
import org.example.sharedprompts.github.port.exception.TemplateException;
import org.example.sharedprompts.github.port.out.BodyTemplatePort;
import org.springframework.stereotype.Component;

/**
 * Prompt Service 기반 BodyTemplate 포트 구현.
 * Custom 템플릿(DB) 또는 기본값(상수) 반환.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromptServiceAdapter implements BodyTemplatePort {

  // 기본 템플릿들
  private static final String DEFAULT_ISSUE_BODY = """
      # {{REPO}} - Issue Template

      ## TL;DR
      {{TITLE}}

      ## Background
      {{COMMITS}}

      ## Requirements
      {{BRANCH}}

      ## Test Plan
      {{DELIVERY_ID}}
      """;

  private static final String DEFAULT_PR_BODY = """
      # {{REPO}} - Pull Request Template

      ## Summary
      {{TITLE}}

      ## What Changed
      {{COMMITS}}

      ## Why
      {{BRANCH}}

      ## Test Plan
      {{DELIVERY_ID}}
      """;

  private final PromptService promptService;

  @Override
  public String resolveTemplate(Long promptId, TemplateKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("kind cannot be null");
    }

    if (promptId == null) {
      return kind == TemplateKind.ISSUE ? DEFAULT_ISSUE_BODY : DEFAULT_PR_BODY;
    }

    try {
      var detail = promptService.getPromptDetail(promptId, null);
      if (detail != null && detail.getContent() != null && !detail.getContent().isBlank()) {
        log.debug("Prompt template loaded - promptId: {}, kind: {}", promptId, kind);
        return detail.getContent();
      }
    } catch (Exception e) {
      log.error("Failed to load prompt template - promptId: {}: {}", promptId, e.getMessage());
      throw new TemplateException("Failed to load prompt template: " + promptId, e);
    }

    log.warn("Prompt template is blank, using default - promptId: {}, kind: {}", promptId, kind);
    return kind == TemplateKind.ISSUE ? DEFAULT_ISSUE_BODY : DEFAULT_PR_BODY;
  }
}
