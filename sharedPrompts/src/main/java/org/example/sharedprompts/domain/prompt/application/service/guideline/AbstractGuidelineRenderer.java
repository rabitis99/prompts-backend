package org.example.sharedprompts.domain.prompt.application.service.guideline;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.GuidelineRule;
import org.example.sharedprompts.domain.prompt.common.guideline.content.GeneralGuidelines;
import org.example.sharedprompts.domain.prompt.common.guideline.rule.RuleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * 가이드라인 렌더러 추상 베이스 클래스
 * <p>공통 렌더링 로직을 제공하고, 언어별 문자열만 추상 메서드로 남긴다.</p>
 */
public abstract class AbstractGuidelineRenderer implements GuidelineRenderer {

    @Override
    public String renderFallbackNotice(TaskDomain domain) {
        if (domain != TaskDomain.GENERAL) {
            return "";
        }

        GuidelineRule notice = GeneralGuidelines.LIMITATION_ACK;
        return "## " + getNoticeTitle(notice) + "\n\n" + getNoticeDescription(notice);
    }

    @Override
    public String renderPersonaHeader(RoleTypeInterface role, ToneType tone, StyleType style) {
        if (role == null || tone == null || style == null) {
            return "";
        }

        return getPersonaHeaderPrefix() +
                role.getRoleNameByLang(getLanguageType()) +
                getPersonaHeaderSuffix() + "\n" +
                renderToneStyleLine(tone, style);
    }

    /**
     * 톤과 스타일 라인 렌더링
     * <p>언어별 특수 처리가 필요한 경우 서브클래스에서 오버라이드할 수 있습니다.</p>
     *
     * @param tone  톤 타입
     * @param style 스타일 타입
     * @return 렌더링된 톤/스타일 라인
     */
    protected String renderToneStyleLine(ToneType tone, StyleType style) {
        if (tone == null || style == null) {
            return "";
        }
        
        return tone.getGuidelineByLang(getLanguageType())
                + getToneStyleConnector()
                + style.getGuidelineByLang(getLanguageType())
                + getPersonaHeaderEnding();
    }

    @Override
    public String renderEssentialConstraints(List<GuidelineRule> hardRules) {
        if (hardRules == null || hardRules.isEmpty()) {
            return "";
        }

        // 단일 패스로 RuleType별 그룹핑
        Map<RuleType, List<GuidelineRule>> groupedByType = hardRules.stream()
                .collect(Collectors.groupingBy(GuidelineRule::type));

        List<GuidelineRule> requires = groupedByType.getOrDefault(RuleType.REQUIRE, List.of());
        List<GuidelineRule> forbids = groupedByType.getOrDefault(RuleType.FORBID, List.of());
        List<GuidelineRule> allows = groupedByType.getOrDefault(RuleType.ALLOW, List.of());

        StringBuilder sb = new StringBuilder();

        if (!requires.isEmpty()) {
            sb.append(requires.stream()
                    .map(this::getRuleDescription)
                    .collect(joining(getDescriptionSeparator())));
        }

        if (!forbids.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            // 제목 없이 설명 + 접미사만 사용 (renderCompactRule과 달리 제목 생략)
            sb.append(forbids.stream()
                    .map(r -> getRuleDescription(r) + getForbidSuffix())
                    .collect(joining(getDescriptionSeparator())));
        }

        if (!allows.isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n");
            // 제목 없이 설명 + 접미사만 사용 (renderCompactRule과 달리 제목 생략)
            sb.append(allows.stream()
                    .map(r -> getRuleDescription(r) + getAllowSuffix())
                    .collect(joining(getDescriptionSeparator())));
        }

        return sb.toString();
    }

    // ========== 언어별 추상 메서드 ==========

    /**
     * 이 렌더러가 사용하는 언어 타입
     */
    protected abstract LanguageType getLanguageType();

    /**
     * 규칙 제목 추출
     */
    protected abstract String getRuleTitle(GuidelineRule rule);

    /**
     * 규칙 설명 추출
     */
    protected abstract String getRuleDescription(GuidelineRule rule);

    /**
     * 폴백 알림 제목 추출
     */
    protected abstract String getNoticeTitle(GuidelineRule notice);

    /**
     * 폴백 알림 설명 추출
     */
    protected abstract String getNoticeDescription(GuidelineRule notice);

    /**
     * 페르소나 헤더 접두사 (예: "당신은 ", "You are ")
     */
    protected abstract String getPersonaHeaderPrefix();

    /**
     * 페르소나 헤더 접미사 (예: "입니다.", ".")
     */
    protected abstract String getPersonaHeaderSuffix();

    /**
     * 톤과 스타일 연결어 (예: "로, ", ", using ")
     */
    protected abstract String getToneStyleConnector();

    /**
     * 페르소나 헤더 종료어 (예: " 형식으로 답변하세요.", ".")
     */
    protected abstract String getPersonaHeaderEnding();

    /**
     * 금지 접미사 (예: " 금지", " Forbidden", " 禁止")
     */
    protected abstract String getForbidSuffix();

    /**
     * 허용 접미사 (예: " 허용", " (Allowed)", " 許可")
     */
    protected abstract String getAllowSuffix();

    /**
     * 설명 구분자 (예: ". ", "。")
     */
    protected abstract String getDescriptionSeparator();
}

