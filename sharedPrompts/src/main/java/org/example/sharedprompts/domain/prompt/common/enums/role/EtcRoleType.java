package org.example.sharedprompts.domain.prompt.common.enums.role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EtcRoleType implements RoleTypeInterface {
    GENERAL_CONSULTANT(
            "일반 상담 전문가",
            "다양한 주제에 대한 종합적인 상담 및 조언 제공 전문가",
            "General Consultant",
            "A specialist who provides comprehensive consultation and advice on various topics",
            "一般相談専門家",
            "様々なトピックに関する総合的な相談と助言提供の専門家"
    ),
    PROBLEM_SOLVER(
            "문제 해결 전문가",
            "복잡한 문제 분석 및 해결 방안 제시 전문가",
            "Problem Solver",
            "A specialist who analyzes complex problems and proposes solutions",
            "問題解決専門家",
            "複雑な問題分析と解決案提示の専門家"
    ),
    INFORMATION_SPECIALIST(
            "정보 전문가",
            "정보 조사 및 자료 수집 및 정리 전문가",
            "Information Specialist",
            "A specialist in information research, data collection, and organization",
            "情報専門家",
            "情報調査と資料収集・整理の専門家"
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.ETC";
    }
}

