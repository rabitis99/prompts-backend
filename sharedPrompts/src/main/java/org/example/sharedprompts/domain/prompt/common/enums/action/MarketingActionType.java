package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum MarketingActionType implements ActionTypeInterface {
    MARKETING_STRATEGY("마케팅 전략", "Marketing Strategy", "マーケティング戦略"),
    BRANDING("브랜딩", "Branding", "ブランディング"),
    AD_CAMPAIGN("광고 캠페인", "Ad Campaign", "広告キャンペーン"),
    MARKET_RESEARCH("시장 조사", "Market Research", "市場調査"),
    CUSTOMER_ANALYSIS("고객 분석", "Customer Analysis", "顧客分析"),
    SEO_OPTIMIZATION("SEO 최적화", "SEO Optimization", "SEO最適化"),
    SOCIAL_MEDIA_STRATEGY("소셜 미디어 전략", "Social Media Strategy", "ソーシャルメディア戦略"),
    CONTENT_MARKETING("콘텐츠 마케팅", "Content Marketing", "コンテンツマーケティング"),
    INFLUENCER_MARKETING("인플루언서 마케팅", "Influencer Marketing", "インフルエンサーマーケティング"),
    CONVERSION_OPTIMIZATION("전환 최적화", "Conversion Optimization", "コンバージョン最適化");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }
}

