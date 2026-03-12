package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 개발 관련 액션 타입 enum.
 *
 * <p>설계 관련 액션(ARCHITECTURE_DESIGN, SYSTEM_DESIGN, TECH_STACK_SELECTION)은
 * 현재 {@link OutputBehaviorType#CODE_IMPLEMENTATION}으로 매핑되어 있습니다.
 * 설계 문서·계획서가 주 결과물이라면 {@link OutputBehaviorType#STRATEGIC_PLAN} 또는
 * 별도 설계용 behavior로의 변경을 검토할 수 있으며, 설계 과정에서 코드 예시가 포함될 수 있어
 * 현재 매핑도 허용 가능합니다.
 */
@Getter
@AllArgsConstructor
public enum DevelopmentActionType implements ActionTypeInterface, StableKeyedEnum {
    ARCHITECTURE_DESIGN("ACTION.DEVELOPMENT.ARCHITECTURE_DESIGN", "아키텍처 설계", "Architecture Design", "アーキテクチャ設計", OutputBehaviorType.CODE_IMPLEMENTATION),
    SYSTEM_DESIGN("ACTION.DEVELOPMENT.SYSTEM_DESIGN", "시스템 설계", "System Design", "システム設計", OutputBehaviorType.CODE_IMPLEMENTATION),
    TECH_STACK_SELECTION("ACTION.DEVELOPMENT.TECH_STACK_SELECTION", "기술 스택 선택", "Tech Stack Selection", "技術スタック選択", OutputBehaviorType.CODE_IMPLEMENTATION),
    PERFORMANCE_OPTIMIZATION("ACTION.DEVELOPMENT.PERFORMANCE_OPTIMIZATION", "성능 최적화", "Performance Optimization", "パフォーマンス最適化", OutputBehaviorType.CODE_IMPLEMENTATION),
    DEPLOYMENT_STRATEGY("ACTION.DEVELOPMENT.DEPLOYMENT_STRATEGY", "배포 전략", "Deployment Strategy", "デプロイ戦略", OutputBehaviorType.STRATEGIC_PLAN),
    DOCUMENTATION("ACTION.DEVELOPMENT.DOCUMENTATION", "문서화", "Documentation", "ドキュメント化", OutputBehaviorType.LONG_FORM_WRITING),
    CODEBASE_ANALYSIS("ACTION.DEVELOPMENT.CODEBASE_ANALYSIS", "코드베이스 분석", "Codebase Analysis", "コードベース分析", OutputBehaviorType.ANALYTICAL_REPORT),
    SECURITY_IMPLEMENTATION("ACTION.DEVELOPMENT.SECURITY_IMPLEMENTATION", "보안 구현", "Security Implementation", "セキュリティ実装", OutputBehaviorType.CODE_IMPLEMENTATION),
    SCALABILITY_PLANNING("ACTION.DEVELOPMENT.SCALABILITY_PLANNING", "확장성 계획", "Scalability Planning", "スケーラビリティ計画", OutputBehaviorType.STRATEGIC_PLAN);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

