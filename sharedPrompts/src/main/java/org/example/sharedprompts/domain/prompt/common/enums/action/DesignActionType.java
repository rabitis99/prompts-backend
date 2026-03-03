package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum DesignActionType implements ActionTypeInterface {
    UI_DESIGN("UI 디자인", "UI Design", "UIデザイン"),
    UX_DESIGN("UX 디자인", "UX Design", "UXデザイン"),
    GRAPHIC_DESIGN("그래픽 디자인", "Graphic Design", "グラフィックデザイン"),
    PRODUCT_DESIGN("제품 디자인", "Product Design", "製品デザイン"),
    DESIGN_DOC("설계 문서", "Design Document", "設計文書"),
    WIREFRAMING("와이어프레임", "Wireframing", "ワイヤーフレーム"),
    PROTOTYPING("프로토타이핑", "Prototyping", "プロトタイピング"),
    VISUAL_IDENTITY("비주얼 아이덴티티", "Visual Identity", "ビジュアルアイデンティティ"),
    INTERACTION_DESIGN("인터랙션 디자인", "Interaction Design", "インタラクションデザイン"),
    RESPONSIVE_DESIGN("반응형 디자인", "Responsive Design", "レスポンシブデザイン");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }
}

