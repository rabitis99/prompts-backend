package org.example.sharedprompts.module.github.port.out;

import org.example.sharedprompts.module.github.port.exception.TemplateException;

/**
 * GitHub 본문 템플릿 해석 포트.
 *
 * 구현 책임:
 * - {@link org.example.sharedprompts.module.github.adapter.out.client.PromptServiceAdapter}
 *
 * LSP 계약:
 * - 템플릿 unresolvable (promptId 미존재 등): exception 발생
 * - 기본값 fallback: 자동으로 default template 반환
 */
public interface BodyTemplatePort {

  /**
   * 본문 템플릿 해석.
   *
   * @param promptId 템플릿 prompt ID (null시 기본값 사용)
   * @param kind "ISSUE" | "PR"
   * @return 마크다운 템플릿
   * @throws IllegalArgumentException kind null
   * @throws TemplateException promptId 존재 안함 (null일 경우 제외)
   */
  String resolveTemplate(Long promptId, TemplateKind kind);

  enum TemplateKind {
    ISSUE, PR
  }
}
