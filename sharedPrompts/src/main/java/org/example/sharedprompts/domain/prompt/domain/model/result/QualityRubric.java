package org.example.sharedprompts.domain.prompt.domain.model.result;

import lombok.Getter;

import java.util.List;

/**
 * 프롬프트 품질 평가 기준 — Verify 단계에서 체크리스트로 사용된다.
 *
 * <p>각 항목은 독립적으로 pass/fail 판정을 받으며,
 * Repair 단계에서는 실패 항목({@link RubricItem})만 지목하여 수정 요청한다.
 */
@Getter
public final class QualityRubric {

    private final List<RubricItem> items;

    private QualityRubric(List<RubricItem> items) {
        this.items = List.copyOf(items);
    }

    public static QualityRubric of(List<RubricItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("QualityRubric에는 최소 1개의 항목이 필요합니다.");
        }
        return new QualityRubric(items);
    }

    /**
     * 품질 루브릭 항목.
     *
     * <ul>
     *   <li>COVERAGE: 요구사항·필수 섹션 누락 없음</li>
     *   <li>INPUT_PRESERVATION: 숫자/조건/엔티티 입력 조건 유지</li>
     *   <li>NO_CONTRADICTION: 논리 모순(A이다 + A가 아니다) 없음</li>
     *   <li>UNCERTAINTY_HANDLING: 검증 불가 수치·인용은 근거 또는 불확실성 명시</li>
     *   <li>FORMAT_COMPLIANCE: 지정 형식(JSON Schema/OutputContract) 준수</li>
     *   <li>NO_PROHIBITED_CONTENT: ContentSandbox 금지어·내용 정책 위반 없음</li>
     * </ul>
     */
    @Getter
    public enum RubricItem {
        COVERAGE(true, "요구사항·필수 섹션 커버리지"),
        INPUT_PRESERVATION(true, "숫자/조건/엔티티 입력 조건 유지"),
        NO_CONTRADICTION(true, "논리 모순 없음"),
        UNCERTAINTY_HANDLING(true, "불확실한 사실에 근거 또는 불확실성 명시"),
        FORMAT_COMPLIANCE(true, "지정 형식(JSON Schema/OutputContract) 준수"),
        NO_PROHIBITED_CONTENT(true, "금지어·내용 정책 위반 없음");

        private final boolean mandatory;
        private final String description;

        RubricItem(boolean mandatory, String description) {
            this.mandatory = mandatory;
            this.description = description;
        }

    }
}
