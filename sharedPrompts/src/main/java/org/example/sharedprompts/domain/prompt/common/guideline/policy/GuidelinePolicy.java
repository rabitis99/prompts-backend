package org.example.sharedprompts.domain.prompt.common.guideline.policy;

import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;

import java.util.List;

/**
 * 가이드라인 정책 인터페이스 — 확장 포인트
 */
public interface GuidelinePolicy {
    List<GuidelineRule> principles();
    List<GuidelineRule> structuringRules();
    List<GuidelineRule> qualityStandards();
    List<GuidelineRule> outputConstraints();
}
