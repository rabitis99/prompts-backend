package org.example.sharedprompts.domain.prompt.common.enums.action.category.design;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
@AllArgsConstructor
public enum DesignActionType implements ActionTypeInterface, StableKeyedEnum {
    UI_DESIGN("UI 디자인", "UI Design", "UIデザイン", ActionGroup.GENERAL_DESIGN),
    UX_DESIGN("UX 디자인", "UX Design", "UXデザイン", ActionGroup.GENERAL_DESIGN),
    GRAPHIC_DESIGN("그래픽 디자인", "Graphic Design", "グラフィックデザイン", ActionGroup.GENERAL_DESIGN),
    PRODUCT_DESIGN("제품 디자인", "Product Design", "製品デザイン", ActionGroup.GENERAL_DESIGN),
    DESIGN_DOC("설계 문서", "Design Document", "設計文書", ActionGroup.GENERAL_DESIGN),
    WIREFRAMING("와이어프레임", "Wireframing", "ワイヤーフレーム", ActionGroup.GENERAL_DESIGN),
    PROTOTYPING("프로토타이핑", "Prototyping", "プロトタイピング", ActionGroup.GENERAL_DESIGN),
    VISUAL_IDENTITY("비주얼 아이덴티티", "Visual Identity", "ビジュアルアイデンティティ", ActionGroup.GENERAL_DESIGN),
    INTERACTION_DESIGN("인터랙션 디자인", "Interaction Design", "インタラクションデザイン", ActionGroup.GENERAL_DESIGN),
    RESPONSIVE_DESIGN("반응형 디자인", "Responsive Design", "レスポンシブデザイン", ActionGroup.GENERAL_DESIGN);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return "ACTION.DESIGN." + name();
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

