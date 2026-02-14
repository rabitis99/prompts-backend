package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;

import java.util.List;

/**
 * 가이드라인 규칙을 텍스트로 렌더링하는 인터페이스
 * <p>정책 결정과 포매팅의 책임을 분리한다.</p>
 */
public interface GuidelineRenderer {

    /**
     * 핵심 원칙 섹션 렌더링
     */
    String renderPrinciples(List<GuidelineRule> rules);

    /**
     * 응답 구조화 규칙 섹션 렌더링
     */
    String renderStructuringRules(List<GuidelineRule> rules);

    /**
     * 응답 품질 기준 섹션 렌더링
     * <p>품질 기준은 출력 제약사항과 통합하여 간소화되었으므로 빈 문자열을 반환합니다.</p>
     *
     * @param rules 규칙 목록 (사용되지 않음)
     * @return 빈 문자열
     */
    default String renderQualityStandards(List<GuidelineRule> rules) {
        // 품질 기준은 출력 제약사항과 통합하여 간소화
        return "";
    }

    /**
     * 출력 형식 제약 섹션 렌더링
     */
    String renderOutputConstraints(List<GuidelineRule> rules);

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

