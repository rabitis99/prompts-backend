package org.example.sharedprompts.domain.prompt.enums.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum DevelopmentActionType implements ActionTypeInterface {
    ARCHITECTURE_DESIGN("아키텍처 설계", "Architecture Design", "アーキテクチャ設計"),
    SYSTEM_DESIGN("시스템 설계", "System Design", "システム設計"),
    TECH_STACK_SELECTION("기술 스택 선택", "Tech Stack Selection", "技術スタック選択"),
    PERFORMANCE_OPTIMIZATION("성능 최적화", "Performance Optimization", "パフォーマンス最適化"),
    DEPLOYMENT_STRATEGY("배포 전략", "Deployment Strategy", "デプロイ戦略"),
    DOCUMENTATION("문서화", "Documentation", "ドキュメント化"),
    CODEBASE_ANALYSIS("코드베이스 분석", "Codebase Analysis", "コードベース分析"),
    SECURITY_IMPLEMENTATION("보안 구현", "Security Implementation", "セキュリティ実装"),
    SCALABILITY_PLANNING("확장성 계획", "Scalability Planning", "スケーラビリティ計画");

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.TECHNICAL);
    }
}

