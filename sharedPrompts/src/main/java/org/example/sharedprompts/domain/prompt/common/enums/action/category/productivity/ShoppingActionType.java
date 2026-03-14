package org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * 쇼핑/상거래 관련 액션 타입 enum
 */
@Getter
@AllArgsConstructor
public enum ShoppingActionType implements ActionTypeInterface, StableKeyedEnum {
    COMPARISON_SHOPPING("ACTION.SHOPPING.COMPARISON_SHOPPING", "상품 비교", "Comparison Shopping", "商品比較", ActionGroup.SHOPPING),
    PRICE_NEGOTIATION("ACTION.SHOPPING.PRICE_NEGOTIATION", "가격 협상", "Price Negotiation", "価格交渉", ActionGroup.SHOPPING);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}
