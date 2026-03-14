package org.example.sharedprompts.domain.prompt.common.enums.action.category.development;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * 개발 관련 액션 타입 enum.
 * Output behavior and task domain are resolved via ActionOutputBehaviorRegistry and ActionDomainRegistry.
 */
@Getter
@AllArgsConstructor
public enum DevelopmentActionType implements ActionTypeInterface, StableKeyedEnum {
    ARCHITECTURE_DESIGN("ACTION.DEVELOPMENT.ARCHITECTURE_DESIGN", "아키텍처 설계", "Architecture Design", "アーキテクチャ設計", ActionGroup.STRATEGY),
    SYSTEM_DESIGN("ACTION.DEVELOPMENT.SYSTEM_DESIGN", "시스템 설계", "System Design", "システム設計", ActionGroup.STRATEGY),
    TECH_STACK_SELECTION("ACTION.DEVELOPMENT.TECH_STACK_SELECTION", "기술 스택 선택", "Tech Stack Selection", "技術スタック選択", ActionGroup.STRATEGY),
    PERFORMANCE_OPTIMIZATION("ACTION.DEVELOPMENT.PERFORMANCE_OPTIMIZATION", "성능 최적화", "Performance Optimization", "パフォーマンス最適化", ActionGroup.PERFORMANCE_OPTIMIZATION),
    DEPLOYMENT_STRATEGY("ACTION.DEVELOPMENT.DEPLOYMENT_STRATEGY", "배포 전략", "Deployment Strategy", "デプロイ戦略", ActionGroup.STRATEGY),
    DOCUMENTATION("ACTION.DEVELOPMENT.DOCUMENTATION", "문서화", "Documentation", "ドキュメント化", ActionGroup.TECHNICAL_WRITING),
    CODEBASE_ANALYSIS("ACTION.DEVELOPMENT.CODEBASE_ANALYSIS", "코드베이스 분석", "Codebase Analysis", "コードベース分析", ActionGroup.CODE_ANALYSIS),
    SECURITY_IMPLEMENTATION("ACTION.DEVELOPMENT.SECURITY_IMPLEMENTATION", "보안 구현", "Security Implementation", "セキュリティ実装", ActionGroup.SECURITY_IMPLEMENTATION),
    SCALABILITY_PLANNING("ACTION.DEVELOPMENT.SCALABILITY_PLANNING", "확장성 계획", "Scalability Planning", "スケーラビリティ計画", ActionGroup.GENERAL_PLANNING);

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

