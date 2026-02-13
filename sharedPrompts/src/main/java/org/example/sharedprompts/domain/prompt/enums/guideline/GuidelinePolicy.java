package org.example.sharedprompts.domain.prompt.enums.guideline;

import java.util.List;

/**
 * 가이드라인 정책 인터페이스 — 확장 포인트
 * <p>현재는 TaskDomain이 직접 구현. 향후 PolicyOverride 레이어 추가 가능.</p>
 */
public interface GuidelinePolicy {
    List<GuidelineRule> principles();
    List<GuidelineRule> structuringRules();
    List<GuidelineRule> qualityStandards();
    List<GuidelineRule> outputConstraints();
}
