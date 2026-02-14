package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.guideline.GuidelineRule;
import org.springframework.stereotype.Component;

/**
 * 한국어 가이드라인 렌더러
 */
@Component
public class KoreanGuidelineRenderer extends AbstractGuidelineRenderer {

    @Override
    protected LanguageType getLanguageType() {
        return LanguageType.KOREAN;
    }

    @Override
    protected String getPrinciplesHeader() {
        return "원칙";
    }

    @Override
    protected String getStructuringRulesHeader() {
        return "응답 규칙";
    }

    @Override
    protected String getRuleTitle(GuidelineRule rule) {
        return rule.title().ko();
    }

    @Override
    protected String getRuleDescription(GuidelineRule rule) {
        return rule.description().ko();
    }

    @Override
    protected String getNoticeTitle(GuidelineRule notice) {
        return notice.title().ko();
    }

    @Override
    protected String getNoticeDescription(GuidelineRule notice) {
        return notice.description().ko();
    }

    @Override
    protected String getPersonaHeaderPrefix() {
        return "당신은 ";
    }

    @Override
    protected String getPersonaHeaderSuffix() {
        return "입니다.";
    }

    @Override
    protected String getToneStyleConnector() {
        return "로, ";
    }

    @Override
    protected String getPersonaHeaderEnding() {
        return " 형식으로 답변하세요.";
    }

    @Override
    protected String getForbidSuffix() {
        return " 금지";
    }

    @Override
    protected String getAllowSuffix() {
        return " 허용";
    }

    @Override
    protected String getDescriptionSeparator() {
        return ". ";
    }

    @Override
    public String renderExperienceContext(ExperienceLevel experience) {
        if (experience == null) {
            return "";
        }
        return switch (experience) {
            case BEGINNER -> "전문 용어를 최소화하고, 기초부터 단계별로 설명하세요.";
            case INTERMEDIATE -> "기본 개념은 가정하고, 실용적 세부사항과 주의점에 집중하세요.";
            case ADVANCED -> "심화 내용, 최적화 기법, 설계 트레이드오프를 포함하세요.";
            case EXPERT -> "최신 동향, 엣지 케이스, 성능 벤치마크, 고급 아키텍처 판단을 다루세요.";
        };
    }
}

