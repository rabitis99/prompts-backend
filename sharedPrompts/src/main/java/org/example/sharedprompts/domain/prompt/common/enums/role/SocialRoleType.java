package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SocialRoleType implements RoleTypeInterface {
    COMMUNITY_MANAGER(
            "커뮤니티 매니저",
            "커뮤니티 구축 및 관리 전문가",
            "Community Manager",
            "A specialist in community building and management",
            "コミュニティマネージャー",
            "コミュニティ構築と管理の専門家"
    ),
    EVENT_COORDINATOR(
            "이벤트 코디네이터",
            "이벤트 기획 및 조직 전문가",
            "Event Coordinator",
            "A specialist in event planning and organization",
            "イベントコーディネーター",
            "イベント企画と組織の専門家"
    ),
    SOCIAL_ACTIVIST(
            "사회 운동가",
            "사회적 원인 옹호 및 변화 추진 전문가",
            "Social Activist",
            "A specialist in social cause advocacy and change promotion",
            "社会活動家",
            "社会的な原因擁護と変化推進の専門家"
    ),
    VOLUNTEER_COORDINATOR(
            "자원봉사 코디네이터",
            "자원봉사 활동 조직 및 관리 전문가",
            "Volunteer Coordinator",
            "A specialist in volunteer activity organization and management",
            "ボランティアコーディネーター",
            "ボランティア活動組織と管理の専門家"
    ),
    FUNDRAISING_SPECIALIST(
            "모금 전문가",
            "모금 전략 수립 및 실행 전문가",
            "Fundraising Specialist",
            "A specialist in fundraising strategy planning and execution",
            "資金調達専門家",
            "資金調達戦略策定と実行の専門家"
    ),
    PUBLIC_RELATIONS_SPECIALIST(
            "PR 전문가",
            "공공 관계 관리 및 커뮤니케이션 전문가",
            "Public Relations Specialist",
            "A specialist in public relations management and communication",
            "PR専門家",
            "公共関係管理とコミュニケーションの専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;
}

