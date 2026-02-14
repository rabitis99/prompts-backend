package org.example.sharedprompts.domain.prompt.service.guideline;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.guideline.DomainResolution;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;
import org.example.sharedprompts.domain.prompt.guideline.RuleLevel;
import org.example.sharedprompts.domain.prompt.service.DomainResolver;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프롬프트 가이드라인 빌더
 * <p>역할/톤/스타일 페르소나 헤더 + 경험 수준 + AI 생성 본문 + 핵심 도메인 제약을 조합하여
 * ~700자 내외의 최종 프롬프트를 구성한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PromptGuidelineBuilder {

    private final GuidelineRendererFactory rendererFactory;
    private final DomainResolver domainResolver;

    /**
     * 최종 프롬프트 구성:
     * 1. 페르소나 헤더 — 역할/톤/스타일 명시 (~150자)
     * 2. 경험 수준 지시 — 응답 깊이 조절 (~50자)
     * 3. AI 생성 본문 — 핵심 프롬프트 (~500자)
     * 4. 필수 제약 — HARD 수준 도메인 규칙 (~100자)
     */
    public String build(String basePrompt, InputRequestDto request) {
        if (basePrompt == null || basePrompt.isBlank()) {
            basePrompt = "";
        }
        
        DomainResolution resolution = resolveDomain(request);
        // language가 null인 경우 기본값으로 KOREAN 사용 (PromptRequestDto.toInputRequestDto()와 동일한 패턴)
        LanguageType language = request.getLanguage() != null ? request.getLanguage() : LanguageType.KOREAN;
        GuidelineRenderer renderer = rendererFactory.getRenderer(language);
        TaskDomain domain = resolution.domain();

        StringBuilder result = new StringBuilder();

        // 1. 페르소나 헤더: 역할 + 톤 + 스타일
        String personaHeader = renderer.renderPersonaHeader(
                request.getRoleType(), request.getTone(), request.getStyle());
        if (!personaHeader.isEmpty()) {
            result.append(personaHeader);
        }

        // 2. 경험 수준 지시 (있는 경우)
        String experienceContext = renderer.renderExperienceContext(request.getExperience());
        if (!experienceContext.isEmpty()) {
            result.append("\n").append(experienceContext);
        }

        if (!result.isEmpty()) {
            result.append("\n\n");
        }

        // 3. AI 생성 본문
        result.append(basePrompt.strip());

        // 4. 필수 도메인 제약 (모든 카테고리의 HARD 수준 규칙)
        List<GuidelineRule> hardRules = extractHardRules(domain);
        if (!hardRules.isEmpty()) {
            String constraints = renderer.renderEssentialConstraints(hardRules);
            if (!constraints.isEmpty()) {
                result.append("\n\n").append(constraints);
            }
        }

        // 5. 폴백 알림 (미매핑 도메인인 경우)
        if (resolution.isFallback()) {
            String fallbackNotice = renderer.renderFallbackNotice(domain);
            if (!fallbackNotice.isEmpty()) {
                result.append("\n\n").append(fallbackNotice);
            }
        }

        return result.toString();
    }

    /**
     * 도메인의 모든 규칙 카테고리에서 HARD 수준인 것만 추출
     * (principles + structuringRules + qualityStandards + outputConstraints)
     */
    private List<GuidelineRule> extractHardRules(TaskDomain domain) {
        return domain.getRulesByLevel(RuleLevel.HARD);
    }

    /**
     * 도메인 결정 (DomainResolver 사용)
     */
    private DomainResolution resolveDomain(InputRequestDto request) {
        return domainResolver.resolveDomain(request);
    }
}
