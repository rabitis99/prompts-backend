package org.example.sharedprompts.domain.prompt.common.enums.role.category.productivity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

/**
 * 생산성 관련 역할 유형 enum
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
 */
@Getter
@AllArgsConstructor
public enum ProductivityRoleType implements RoleTypeInterface {
    PRODUCTIVITY_EXPERT(
            "생산성 전문가",                    // roleNameKo
            "효율성을 극대화하고 작업 프로세스를 최적화하는 전문가",  // descriptionKo
            "Productivity Expert",            // roleNameEn
            "A specialist who maximizes efficiency and optimizes work processes", // descriptionEn
            "生産性専門家",                      // roleNameJa
            "効率性を最大化し、作業プロセスを最適化する専門家"    // descriptionJa
    ),
    TIME_MANAGEMENT_SPECIALIST(
            "시간 관리 전문가",                    // roleNameKo
            "효과적인 시간 관리 및 일정 계획 수립 전문가",      // descriptionKo
            "Time Management Specialist",        // roleNameEn
            "A specialist in effective time management and schedule planning", // descriptionEn
            "時間管理専門家",                      // roleNameJa
            "効果的な時間管理とスケジュール計画策定の専門家"      // descriptionJa
    ),
    WORKFLOW_OPTIMIZER(
            "워크플로우 최적화 전문가",            // roleNameKo
            "작업 흐름 분석 및 프로세스 최적화 전문가",    // descriptionKo
            "Workflow Optimizer",                // roleNameEn
            "A specialist in workflow analysis and process optimization", // descriptionEn
            "ワークフロー最適化専門家",                // roleNameJa
            "作業フロー分析とプロセス最適化の専門家"        // descriptionJa
    ),
    AUTOMATION_SPECIALIST(
            "자동화 전문가",                      // roleNameKo
            "반복 작업 자동화 및 효율성 향상 전문가",    // descriptionKo
            "Automation Specialist",            // roleNameEn
            "A specialist in automating repetitive tasks and improving efficiency", // descriptionEn
            "自動化専門家",                        // roleNameJa
            "反復作業自動化と効率性向上の専門家"          // descriptionJa
    ),
    EFFICIENCY_CONSULTANT(
            "효율성 컨설턴트",                      // roleNameKo
            "업무 효율성 분석 및 개선 방안 제시 전문가",    // descriptionKo
            "Efficiency Consultant",            // roleNameEn
            "A specialist in analyzing work efficiency and proposing improvement solutions", // descriptionEn
            "効率性コンサルタント",                    // roleNameJa
            "業務効率性分析と改善案提示の専門家"          // descriptionJa
    );

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;

    @Override
    public String keyPrefix() {
        return "ROLE.PRODUCTIVITY";
    }
}

