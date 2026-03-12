package org.example.sharedprompts.domain.prompt.common.enums.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

/**
 * ActionType이 궁극적으로 요구하는 출력 행동을 추상화한 코어 enum.
 *
 * <p>각 값은 다음을 암시한다.</p>
 * <ul>
 *   <li>대표 TaskDomain</li>
 *   <li>전형적인 응답 구조(ResponseStructure)</li>
 *   <li>기본 출력 포맷(OutputFormat)</li>
 * </ul>
 *
 * <p>세부 ActionType enum들은 각 상수에 OutputBehaviorType을 보유하고
 * {@link org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface#getOutputBehavior()}로 노출한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OutputBehaviorType {

    /** 일반 상담/설명형 응답 (Q&A, 조언 등) */
    GENERAL_CONSULTATION(
            TaskDomain.GENERAL,
            ResponseStructure.PARAGRAPH,
            OutputFormat.MARKDOWN
    ),

    /** 장문 콘텐츠 작성 (기사, 에세이, 리포트 등) */
    LONG_FORM_WRITING(
            TaskDomain.CREATIVE,
            ResponseStructure.PARAGRAPH,
            OutputFormat.MARKDOWN
    ),

    /** 짧은 카피/헤드라인/캡션 등 단문 위주의 작성 */
    SHORT_COPY(
            TaskDomain.CREATIVE,
            ResponseStructure.BULLET_LIST,
            OutputFormat.MARKDOWN
    ),

    /** 이메일, 편지, DM 등 1:1 커뮤니케이션 메시지 */
    MESSAGE_COMPOSITION(
            TaskDomain.PRACTICAL,
            ResponseStructure.PARAGRAPH,
            OutputFormat.PLAIN_TEXT
    ),

    /** 개념 설명, 튜토리얼, How-to 가이드 */
    EDUCATIONAL_EXPLANATION(
            TaskDomain.EDUCATIONAL,
            ResponseStructure.STEP_BY_STEP,
            OutputFormat.MARKDOWN
    ),

    /** 계획/로드맵/전략 수립 (업무, 학습, 커리어 등) */
    STRATEGIC_PLAN(
            TaskDomain.PRACTICAL,
            ResponseStructure.BULLET_LIST,
            OutputFormat.MARKDOWN
    ),

    /** 비교/평가/의견을 포함한 분석 리포트 */
    ANALYTICAL_REPORT(
            TaskDomain.ANALYTICAL,
            ResponseStructure.MIXED,
            OutputFormat.MARKDOWN
    ),

    /** 데이터/지표 중심의 분석(표, 요약 지표 등) */
    DATA_ANALYSIS(
            TaskDomain.ANALYTICAL,
            ResponseStructure.TABLE,
            OutputFormat.MARKDOWN
    ),

    /** 코드/스크립트/쿼리 등 구현 중심 응답 */
    CODE_IMPLEMENTATION(
            TaskDomain.TECHNICAL,
            ResponseStructure.CODE_WITH_EXPLANATION,
            OutputFormat.CODE
    ),

    /** 버그 진단, 원인 분석, 수정 제안 */
    DEBUGGING_SESSION(
            TaskDomain.TECHNICAL,
            ResponseStructure.STEP_BY_STEP,
            OutputFormat.CODE
    ),

    /** 코드 리뷰, 리팩토링 제안, 베스트 프랙티스 피드백 */
    CODE_REVIEW_FEEDBACK(
            TaskDomain.TECHNICAL,
            ResponseStructure.BULLET_LIST,
            OutputFormat.MARKDOWN
    ),

    /** 추천 리스트, 랭킹, 옵션 비교 */
    RECOMMENDATION_LIST(
            TaskDomain.GENERAL,
            ResponseStructure.BULLET_LIST,
            OutputFormat.MARKDOWN
    ),

    /** 구조화된 Q&A 형식 응답 */
    QA_STYLE_RESPONSE(
            TaskDomain.GENERAL,
            ResponseStructure.QA_PAIRS,
            OutputFormat.MARKDOWN
    );

    /** 기본 TaskDomain 힌트 */
    private final TaskDomain defaultTaskDomain;
    /** 전형적인 응답 구조 */
    private final ResponseStructure responseStructure;
    /** 기본 출력 포맷 */
    private final OutputFormat outputFormat;
}
