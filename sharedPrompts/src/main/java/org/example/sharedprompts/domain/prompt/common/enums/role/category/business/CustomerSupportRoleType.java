package org.example.sharedprompts.domain.prompt.common.enums.role.category.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

/**
 * 고객 지원 관련 역할 유형 enum
 * 
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>roleNameKo - 역할 이름 (한국어)</li>
 *   <li>descriptionKo - 역할 설명 (한국어)</li>
 *   <li>roleNameEn - 역할 이름 (영어)</li>
 *   <li>descriptionEn - 역할 설명 (영어)</li>
 *   <li>roleNameJa - 역할 이름 (일본어)</li>
 *   <li>descriptionJa - 역할 설명 (일본어)</li>
 * </ol>
 * 
 * <p>주의: 모든 파라미터가 String 타입이므로 순서를 정확히 지켜야 합니다.
 * 향후 리팩토링 시 Builder 패턴이나 record 기반 구조를 고려할 수 있습니다.
 */
@Getter
@AllArgsConstructor
public enum CustomerSupportRoleType implements RoleTypeInterface {
    CUSTOMER_SUPPORT_SPECIALIST(
            "고객 지원 전문가",                    // roleNameKo
            "고객 문의 응대 및 문제 해결 전문가",      // descriptionKo
            "Customer Support Specialist",        // roleNameEn
            "A specialist in customer inquiry response and problem resolution", // descriptionEn
            "カスタマーサポート専門家",              // roleNameJa
            "顧客問い合わせ対応と問題解決の専門家"      // descriptionJa
    ),
    CUSTOMER_SUCCESS_MANAGER(
            "고객 성공 관리자",                    // roleNameKo
            "고객 온보딩 및 성공 관리 전문가",        // descriptionKo
            "Customer Success Manager",            // roleNameEn
            "A specialist in customer onboarding and success management", // descriptionEn
            "カスタマーサクセスマネージャー",          // roleNameJa
            "顧客オンボーディングと成功管理の専門家"    // descriptionJa
    ),
    TECHNICAL_SUPPORT_ENGINEER(
            "기술 지원 엔지니어",                    // roleNameKo
            "기술적 문제 해결 및 지원 제공 전문가",    // descriptionKo
            "Technical Support Engineer",          // roleNameEn
            "A specialist in technical problem solving and support provision", // descriptionEn
            "テクニカルサポートエンジニア",            // roleNameJa
            "技術的問題解決とサポート提供の専門家"      // descriptionJa
    ),
    SUPPORT_TRAINER(
            "지원 교육 전문가",                      // roleNameKo
            "고객 지원 팀 교육 및 역량 강화 전문가",    // descriptionKo
            "Support Trainer",                      // roleNameEn
            "A specialist in customer support team training and capability enhancement", // descriptionEn
            "サポート教育専門家",                      // roleNameJa
            "カスタマーサポートチーム教育と能力強化の専門家" // descriptionJa
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.CUSTOMER_SUPPORT";
    }
}

