package org.example.sharedprompts.domain.prompt.domain.objective;

import org.example.sharedprompts.domain.prompt.domain.model.spec.Constraints;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.strategy.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityPriority;
import org.example.sharedprompts.domain.prompt.domain.verification.VerificationStrategy;
import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;

import java.util.List;

/**
 * Objective당 모든 행위를 캡슐화하는 프로파일 인터페이스.
 *
 * <p>새 Objective 추가 = 이 인터페이스를 구현하는 클래스 1개 + 레지스트리 등록 1줄.
 * 기존 클래스는 수정 불필요(OCP).
 *
 * <p>Spring 의존 금지 — 구현체는 순수 Java 클래스여야 한다.
 */
public interface ObjectiveProfile {

    /** 이 프로파일이 담당하는 Objective */
    PromptObjective objective();

    /** Solve(1회) 포함 총 LLM 호출 상한 */
    int maxLlmCallCount();

    /** true면 Solve 단계에서 ConstrainedDecodingPort를 우선 사용 (EXTRACTION) */
    boolean supportsConstrainedDecoding();

    /** 이 Objective의 품질 우선순위 */
    QualityPriority priority();

    /** Verify 단계에서 사용할 루브릭 항목 목록 */
    QualityRubric rubric();

    /**
     * 경험 수준을 반영한 출력 제약 조건.
     * 공통 스케일링(BEGINNER=1.2×, EXPERT=0.75×)은 구현체가 직접 적용한다.
     */
    Constraints constraints(ExperienceLevel level);

    /**
     * 출력 형식 계약.
     *
     * @param jsonSchema EXTRACTION 시 사용자 제공 스키마 (null이면 기본값 적용)
     * @param maxTokens  constraints에서 계산된 최대 토큰 수
     */
    OutputContract outputContract(String jsonSchema, int maxTokens);

    /** 이 Objective 전용 검증 전략 — switch/case 대신 이 메서드로 디스패치 */
    VerificationStrategy verificationStrategy();

    /** experimentalEnabled 미적용(필터링 전)의 기본 전략 번들 */
    PromptStrategyBundle defaultBundle();

    /**
     * Objective별 추가 섹션 (공통 Role·Checklist·Instruction 이후에 삽입).
     * 예: EXTRACTION → OUTPUT_FORMAT 섹션, PLANNING → CONSTRAINTS 섹션
     */
    List<PromptSection> extraSections();

    /** Objective별 지시 방향 문구 (Renderer가 메타프롬프트에 포함) */
    String instructionContent(LanguageType locale);
}
