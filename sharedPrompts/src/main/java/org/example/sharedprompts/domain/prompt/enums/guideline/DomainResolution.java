package org.example.sharedprompts.domain.prompt.enums.guideline;

import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

/**
 * 도메인 결정 결과 — 폴백 여부 포함
 *
 * @param domain     결정된 TaskDomain
 * @param isFallback true이면 미매핑으로 인한 GENERAL 폴백 (경고 로그 대상)
 */
public record DomainResolution(TaskDomain domain, boolean isFallback) {
}
