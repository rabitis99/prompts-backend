package org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum MarketingActionType implements ActionTypeInterface, StableKeyedEnum {
    MARKETING_STRATEGY("마케팅 전략", "Marketing Strategy", "マーケティング戦略", OutputBehaviorType.STRATEGIC_PLAN),
    BRANDING("브랜딩", "Branding", "ブランディング", OutputBehaviorType.STRATEGIC_PLAN),
    AD_CAMPAIGN("광고 캠페인", "Ad Campaign", "広告キャンペーン", OutputBehaviorType.LONG_FORM_WRITING),
    MARKET_RESEARCH("시장 조사", "Market Research", "市場調査", OutputBehaviorType.ANALYTICAL_REPORT),
    CUSTOMER_ANALYSIS("고객 분석", "Customer Analysis", "顧客分析", OutputBehaviorType.ANALYTICAL_REPORT),
    SEO_OPTIMIZATION("SEO 최적화", "SEO Optimization", "SEO最適化", OutputBehaviorType.STRATEGIC_PLAN),
    SOCIAL_MEDIA_STRATEGY("소셜 미디어 전략", "Social Media Strategy", "ソーシャルメディア戦略", OutputBehaviorType.STRATEGIC_PLAN),
    CONTENT_MARKETING("콘텐츠 마케팅", "Content Marketing", "コンテンツマーケティング", OutputBehaviorType.LONG_FORM_WRITING),
    INFLUENCER_MARKETING("인플루언서 마케팅", "Influencer Marketing", "インフルエンサーマーケティング", OutputBehaviorType.LONG_FORM_WRITING),
    CONVERSION_OPTIMIZATION("전환 최적화", "Conversion Optimization", "コンバージョン最適化", OutputBehaviorType.STRATEGIC_PLAN);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.MARKETING." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

