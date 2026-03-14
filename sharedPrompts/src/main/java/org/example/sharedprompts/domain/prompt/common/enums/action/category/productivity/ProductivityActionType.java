package org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

@Getter
public enum ProductivityActionType implements ActionTypeInterface, StableKeyedEnum {
    WORKFLOW_OPTIMIZATION("ACTION.PRODUCTIVITY.WORKFLOW_OPTIMIZATION", "워크플로우 최적화", "Workflow Optimization", "ワークフロー最適化", ActionGroup.PERSONAL_PRODUCTIVITY),
    TIME_MANAGEMENT("ACTION.PRODUCTIVITY.TIME_MANAGEMENT", "시간 관리", "Time Management", "時間管理", ActionGroup.PERSONAL_PRODUCTIVITY),
    SCHEDULE_PLANNING("ACTION.PRODUCTIVITY.SCHEDULE_PLANNING", "일정 계획", "Schedule Planning", "スケジュール計画", ActionGroup.GENERAL_PLANNING),
    DAILY_PLANNING("ACTION.PRODUCTIVITY.DAILY_PLANNING", "하루 계획", "Daily Planning", "一日の計画", ActionGroup.GENERAL_PLANNING),
    WEEKLY_PLANNING("ACTION.PRODUCTIVITY.WEEKLY_PLANNING", "주간 계획", "Weekly Planning", "週間計画", ActionGroup.GENERAL_PLANNING),
    MONTHLY_PLANNING("ACTION.PRODUCTIVITY.MONTHLY_PLANNING", "월간 계획", "Monthly Planning", "月間計画", ActionGroup.GENERAL_PLANNING),
    TASK_AUTOMATION("ACTION.PRODUCTIVITY.TASK_AUTOMATION", "작업 자동화", "Task Automation", "タスク自動化", ActionGroup.PERSONAL_PRODUCTIVITY),
    SHOPPING_LIST("ACTION.PRODUCTIVITY.SHOPPING_LIST", "쇼핑 리스트", "Shopping List", "買い物リスト", ActionGroup.GENERAL_PLANNING),
    MEAL_PLANNING("ACTION.PRODUCTIVITY.MEAL_PLANNING", "식사 계획", "Meal Planning", "食事計画", ActionGroup.GENERAL_PLANNING),
    BUDGET_PLANNING("ACTION.PRODUCTIVITY.BUDGET_PLANNING", "예산 계획", "Budget Planning", "予算計画", ActionGroup.GENERAL_PLANNING),
    HOUSEHOLD_MANAGEMENT("ACTION.PRODUCTIVITY.HOUSEHOLD_MANAGEMENT", "가정 관리", "Household Management", "家事管理", ActionGroup.PERSONAL_PRODUCTIVITY),
    EFFICIENCY_ANALYSIS("ACTION.PRODUCTIVITY.EFFICIENCY_ANALYSIS", "효율성 분석", "Efficiency Analysis", "効率性分析", ActionGroup.DATA_ANALYSIS),
    PRODUCTIVITY_PLANNING("ACTION.PRODUCTIVITY.PRODUCTIVITY_PLANNING", "생산성 계획 수립", "Productivity Planning", "生産性計画策定", ActionGroup.GENERAL_PLANNING),
    PROCESS_IMPROVEMENT("ACTION.PRODUCTIVITY.PROCESS_IMPROVEMENT", "프로세스 개선", "Process Improvement", "プロセス改善", ActionGroup.PERSONAL_PRODUCTIVITY),
    RESOURCE_OPTIMIZATION("ACTION.PRODUCTIVITY.RESOURCE_OPTIMIZATION", "자원 최적화", "Resource Optimization", "リソース最適化", ActionGroup.PERSONAL_PRODUCTIVITY);

    private final String stableKey;
    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final ActionGroup actionGroup;

    ProductivityActionType(String stableKey, String displayNameKo, String displayNameEn, String displayNameJa, ActionGroup actionGroup) {
        this.stableKey = stableKey;
        this.displayNameKo = displayNameKo;
        this.displayNameEn = displayNameEn;
        this.displayNameJa = displayNameJa;
        this.actionGroup = actionGroup;
    }

    @Override
    public String key() {
        return stableKey;
    }

    @Override
    public ActionGroup getActionGroup() {
        return actionGroup;
    }
}

