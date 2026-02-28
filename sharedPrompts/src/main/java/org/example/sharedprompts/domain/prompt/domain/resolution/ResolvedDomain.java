package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

/**
 * 도메인 해석 결과 — 결정된 도메인과 폴백 사용 여부.
 * {@link DomainResolver#resolveDomainWithFallback}의 반환 타입.
 */
public record ResolvedDomain(TaskDomain domain, boolean isFallback) {
}
