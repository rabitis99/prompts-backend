package org.example.sharedprompts.domain.prompt.common.guideline.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

/**
 * 도메인 결정 결과 — 폴백 여부 포함
 */
public record DomainResolution(TaskDomain domain, boolean isFallback) {
}
