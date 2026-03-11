package org.example.sharedprompts.domain.prompt.common.enums.role.category.design;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

@Getter
@AllArgsConstructor
public enum DesignRoleType implements RoleTypeInterface, StableKeyedEnum {
    UI_UX_DESIGNER(
            "UI/UX 디자이너",
            "사용자 경험을 고려한 인터페이스 설계 및 디자인 전문가",
            "UI/UX Designer",
            "A specialist in interface design and design considering user experience",
            "UI/UXデザイナー",
            "ユーザー体験を考慮したインターフェース設計とデザインの専門家"
    ),
    GRAPHIC_DESIGNER(
            "그래픽 디자이너",
            "시각적 커뮤니케이션을 위한 그래픽 디자인 전문가",
            "Graphic Designer",
            "A specialist in graphic design for visual communication",
            "グラフィックデザイナー",
            "視覚的コミュニケーションのためのグラフィックデザイン専門家"
    ),
    PRODUCT_DESIGNER(
            "제품 디자이너",
            "제품 설계 및 사용자 중심 디자인 전문가",
            "Product Designer",
            "A specialist in product design and user-centered design",
            "製品デザイナー",
            "製品設計とユーザー中心デザインの専門家"
    ),
    INTERACTION_DESIGNER(
            "인터랙션 디자이너",
            "사용자 인터랙션 설계 및 프로토타이핑 전문가",
            "Interaction Designer",
            "A specialist in user interaction design and prototyping",
            "インタラクションデザイナー",
            "ユーザーインタラクション設計とプロトタイピングの専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.DESIGN";
    }
}

