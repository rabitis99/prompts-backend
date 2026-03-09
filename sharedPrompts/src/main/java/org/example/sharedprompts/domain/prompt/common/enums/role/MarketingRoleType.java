package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

@Getter
@AllArgsConstructor
public enum MarketingRoleType implements RoleTypeInterface, StableKeyedEnum {
    MARKETING_STRATEGIST(
            "마케팅 전략가",
            "시장 분석, 고객 분석 및 광고 캠페인 기획 전문가",
            "Marketing Strategist",
            "A specialist in market analysis, customer analysis, and advertising campaign planning",
            "マーケティング戦略家",
            "市場分析、顧客分析、広告キャンペーン企画の専門家"
    ),
    BRAND_SPECIALIST(
            "브랜드 전문가",
            "브랜드 아이덴티티 구축 및 브랜딩 전략 수립 전문가",
            "Brand Specialist",
            "A specialist in building brand identity and establishing branding strategies",
            "ブランド専門家",
            "ブランドアイデンティティ構築とブランディング戦略策定の専門家"
    ),
    DIGITAL_MARKETER(
            "디지털 마케터",
            "디지털 채널을 통한 마케팅 전략 수립 및 실행 전문가",
            "Digital Marketer",
            "A specialist in developing and executing marketing strategies through digital channels",
            "デジタルマーケター",
            "デジタルチャネルを通じたマーケティング戦略策定と実行の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.MARKETING";
    }
}

