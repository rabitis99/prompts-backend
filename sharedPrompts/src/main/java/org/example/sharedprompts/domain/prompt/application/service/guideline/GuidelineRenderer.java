package org.example.sharedprompts.domain.prompt.application.service.guideline;

import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;

public interface GuidelineRenderer {

    /**
     * GENERAL 폴백 시 한계 인정 안내 문구 렌더링
     */
    String renderFallbackNotice(TaskDomain domain);

    /**
     * 역할/톤/스타일을 포함한 페르소나 헤더 렌더링
     * <p>최종 출력의 첫 부분에 배치되어 AI의 역할, 어조, 스타일을 명시한다.</p>
     */
    String renderPersonaHeader(RoleTypeInterface role, ToneType tone, StyleType style);

    /**
     * HARD 수준의 핵심 도메인 규칙만 간결하게 렌더링
     * <p>출력 제약에 맞게 최소한의 필수 규칙만 포함한다.</p>
     */
    String renderEssentialConstraints(List<GuidelineRule> hardRules);

    /**
     * 경험 수준에 따른 응답 깊이 지시 렌더링
     * <p>null인 경우 빈 문자열을 반환한다.</p>
     */
    String renderExperienceContext(ExperienceLevel experience);
}

