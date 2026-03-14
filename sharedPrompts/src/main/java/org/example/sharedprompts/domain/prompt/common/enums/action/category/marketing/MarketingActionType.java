package org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum MarketingActionType implements ActionTypeInterface, StableKeyedEnum {
    MARKETING_STRATEGY("마케팅 전략", "Marketing Strategy", "マーケティング戦略", ActionGroup.MARKETING_STRATEGY),
    BRANDING("브랜딩", "Branding", "ブランディング", ActionGroup.MARKETING_STRATEGY),
    AD_CAMPAIGN("광고 캠페인", "Ad Campaign", "広告キャンペーン", ActionGroup.MARKETING_EXECUTION),
    MARKET_RESEARCH("시장 조사", "Market Research", "市場調査", ActionGroup.DATA_ANALYSIS),
    CUSTOMER_ANALYSIS("고객 분석", "Customer Analysis", "顧客分析", ActionGroup.DATA_ANALYSIS),
    SEO_OPTIMIZATION("SEO 최적화", "SEO Optimization", "SEO最適化", ActionGroup.MARKETING_EXECUTION),
    SOCIAL_MEDIA_STRATEGY("소셜 미디어 전략", "Social Media Strategy", "ソーシャルメディア戦略", ActionGroup.MARKETING_STRATEGY),
    CONTENT_MARKETING("콘텐츠 마케팅", "Content Marketing", "コンテンツマーケティング", ActionGroup.LONG_FORM_WRITING),
    INFLUENCER_MARKETING("인플루언서 마케팅", "Influencer Marketing", "インフルエンサーマーケティング", ActionGroup.MARKETING_EXECUTION),
    CONVERSION_OPTIMIZATION("전환 최적화", "Conversion Optimization", "コンバージョン最適化", ActionGroup.MARKETING_EXECUTION);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.MARKETING." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

