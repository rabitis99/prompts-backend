package org.example.sharedprompts.domain.prompt.common.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

import java.util.Optional;

/**
 * 쇼핑/상거래 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum ShoppingActionType implements ActionTypeInterface, StableKeyedEnum {
    COMPARISON_SHOPPING("ACTION.SHOPPING.COMPARISON_SHOPPING", "상품 비교", "Comparison Shopping", "商品比較"),
    PRICE_NEGOTIATION("ACTION.SHOPPING.PRICE_NEGOTIATION", "가격 협상", "Price Negotiation", "価格交渉");

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }
}

