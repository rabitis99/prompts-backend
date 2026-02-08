package org.example.sharedprompts.domain.prompt.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DevelopmentRoleType implements RoleTypeInterface {
    BACKEND_DEVELOPER(
            "백엔드 개발자",
            "시스템 아키텍처 설계, 기술 스택 선택 및 성능 최적화 전문가",
            "Backend Developer",
            "A specialist in system architecture design, technology stack selection, and performance optimization",
            "バックエンド開発者",
            "システムアーキテクチャ設計、技術スタック選択、パフォーマンス最適化の専門家"
    ),
    FRONTEND_DEVELOPER(
            "프론트엔드 개발자",
            "사용자 인터페이스 개발 및 사용자 경험 최적화 전문가",
            "Frontend Developer",
            "A specialist in user interface development and user experience optimization",
            "フロントエンド開発者",
            "ユーザーインターフェース開発とユーザー体験最適化の専門家"
    ),
    FULL_STACK_DEVELOPER(
            "풀스택 개발자",
            "프론트엔드와 백엔드 전반에 걸친 종합적인 개발 전문가",
            "Full Stack Developer",
            "A comprehensive development specialist covering both frontend and backend",
            "フルスタック開発者",
            "フロントエンドとバックエンド全般にわたる総合的な開発専門家"
    ),
    DEVOPS_ENGINEER(
            "DevOps 엔지니어",
            "배포 자동화, 인프라 관리 및 CI/CD 파이프라인 구축 전문가",
            "DevOps Engineer",
            "A specialist in deployment automation, infrastructure management, and CI/CD pipeline construction",
            "DevOpsエンジニア",
            "デプロイ自動化、インフラ管理、CI/CDパイプライン構築の専門家"
    ),
    CLOUD_ARCHITECT(
            "클라우드 아키텍트",
            "클라우드 인프라 설계 및 최적화 전문가",
            "Cloud Architect",
            "A specialist in cloud infrastructure design and optimization",
            "クラウドアーキテクト",
            "クラウドインフラ設計と最適化の専門家"
    ),
    SITE_RELIABILITY_ENGINEER(
            "SRE 엔지니어",
            "시스템 안정성 및 신뢰성 보장 전문가",
            "Site Reliability Engineer",
            "A specialist in ensuring system stability and reliability",
            "SREエンジニア",
            "システム安定性と信頼性保証の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;
}

