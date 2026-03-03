package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

/**
 * TaskDomain 해석 결과 (도메인 + 폴백 사용 여부).
 *
 * <p>공개 API. Spring/프레임워크 무의존.
 */
public record ResolvedDomain(TaskDomain domain, boolean isFallback) {
}
