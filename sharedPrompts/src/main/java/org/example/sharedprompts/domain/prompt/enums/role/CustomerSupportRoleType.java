package org.example.sharedprompts.domain.prompt.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomerSupportRoleType implements RoleTypeInterface {
    CUSTOMER_SUPPORT_SPECIALIST(
            "고객 지원 전문가",
            "고객 문의 응대 및 문제 해결 전문가",
            "Customer Support Specialist",
            "A specialist in customer inquiry response and problem resolution",
            "カスタマーサポート専門家",
            "顧客問い合わせ対応と問題解決の専門家"
    ),
    CUSTOMER_SUCCESS_MANAGER(
            "고객 성공 관리자",
            "고객 온보딩 및 성공 관리 전문가",
            "Customer Success Manager",
            "A specialist in customer onboarding and success management",
            "カスタマーサクセスマネージャー",
            "顧客オンボーディングと成功管理の専門家"
    ),
    TECHNICAL_SUPPORT_ENGINEER(
            "기술 지원 엔지니어",
            "기술적 문제 해결 및 지원 제공 전문가",
            "Technical Support Engineer",
            "A specialist in technical problem solving and support provision",
            "テクニカルサポートエンジニア",
            "技術的問題解決とサポート提供の専門家"
    ),
    SUPPORT_TRAINER(
            "지원 교육 전문가",
            "고객 지원 팀 교육 및 역량 강화 전문가",
            "Support Trainer",
            "A specialist in customer support team training and capability enhancement",
            "サポート教育専門家",
            "カスタマーサポートチーム教育と能力強化の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;
}

