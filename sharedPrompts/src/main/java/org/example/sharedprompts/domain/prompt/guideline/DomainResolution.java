package org.example.sharedprompts.domain.prompt.guideline;

import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

/**
 * 도메인 결정 결과 — 폴백 여부 포함
 */
public record DomainResolution(TaskDomain domain, boolean isFallback) {
}

