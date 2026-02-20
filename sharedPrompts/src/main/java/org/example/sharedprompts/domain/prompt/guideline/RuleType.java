package org.example.sharedprompts.domain.prompt.guideline;

/**
 * 규칙 방향 — REQUIRE(해야 한다) / FORBID(하지 마라) / ALLOW(해도 된다)
 */
public enum RuleType {
    /** ~해야 한다 */
    REQUIRE,
    /** ~하지 마라 */
    FORBID,
    /** ~해도 된다 (다른 규칙이 금지해도 이 도메인에서는 허용) */
    ALLOW
}
