package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;

@Getter
@AllArgsConstructor
public enum ContentRoleType implements RoleTypeInterface, StableKeyedEnum {
    CONTENT_CREATOR(
            "ROLE.CONTENT.CONTENT_CREATOR",
            "콘텐츠 제작자",
            "콘텐츠 생성 및 편집을 통한 브랜드 강화 전문가",
            "Content Creator",
            "A specialist who strengthens brands through content creation and editing",
            "コンテンツ制作者",
            "コンテンツ作成と編集によるブランド強化の専門家"
    ),
    CONTENT_STRATEGIST(
            "ROLE.CONTENT.CONTENT_STRATEGIST",
            "콘텐츠 전략가",
            "콘텐츠 기획 및 전략 수립을 담당하는 전문가",
            "Content Strategist",
            "A specialist responsible for content planning and strategy development",
            "コンテンツ戦略家",
            "コンテンツ企画と戦略策定を担当する専門家"
    ),
    SOCIAL_MEDIA_MANAGER(
            "ROLE.CONTENT.SOCIAL_MEDIA_MANAGER",
            "소셜 미디어 매니저",
            "소셜 미디어 콘텐츠 기획 및 커뮤니티 관리 전문가",
            "Social Media Manager",
            "A specialist in social media content planning and community management",
            "ソーシャルメディアマネージャー",
            "ソーシャルメディアコンテンツ企画とコミュニティ管理の専門家"
    );

    private final String stableKey;
    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String key() {
        return stableKey;
    }
}

