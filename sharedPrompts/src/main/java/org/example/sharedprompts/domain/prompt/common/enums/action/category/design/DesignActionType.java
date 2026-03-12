package org.example.sharedprompts.domain.prompt.common.enums.action.category.design;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum DesignActionType implements ActionTypeInterface, StableKeyedEnum {
    UI_DESIGN("UI 디자인", "UI Design", "UIデザイン", OutputBehaviorType.LONG_FORM_WRITING),
    UX_DESIGN("UX 디자인", "UX Design", "UXデザイン", OutputBehaviorType.LONG_FORM_WRITING),
    GRAPHIC_DESIGN("그래픽 디자인", "Graphic Design", "グラフィックデザイン", OutputBehaviorType.LONG_FORM_WRITING),
    PRODUCT_DESIGN("제품 디자인", "Product Design", "製品デザイン", OutputBehaviorType.LONG_FORM_WRITING),
    DESIGN_DOC("설계 문서", "Design Document", "設計文書", OutputBehaviorType.LONG_FORM_WRITING),
    WIREFRAMING("와이어프레임", "Wireframing", "ワイヤーフレーム", OutputBehaviorType.LONG_FORM_WRITING),
    PROTOTYPING("프로토타이핑", "Prototyping", "プロトタイピング", OutputBehaviorType.LONG_FORM_WRITING),
    VISUAL_IDENTITY("비주얼 아이덴티티", "Visual Identity", "ビジュアルアイデンティティ", OutputBehaviorType.LONG_FORM_WRITING),
    INTERACTION_DESIGN("인터랙션 디자인", "Interaction Design", "インタラクションデザイン", OutputBehaviorType.LONG_FORM_WRITING),
    RESPONSIVE_DESIGN("반응형 디자인", "Responsive Design", "レスポンシブデザイン", OutputBehaviorType.LONG_FORM_WRITING);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.DESIGN." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.CREATIVE);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

