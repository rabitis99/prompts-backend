package org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ProductivityActionType implements ActionTypeInterface, StableKeyedEnum {
    WORKFLOW_OPTIMIZATION("워크플로우 최적화", "Workflow Optimization", "ワークフロー最適化", OutputBehaviorType.STRATEGIC_PLAN),
    TIME_MANAGEMENT("시간 관리", "Time Management", "時間管理", OutputBehaviorType.STRATEGIC_PLAN),
    SCHEDULE_PLANNING("일정 계획", "Schedule Planning", "スケジュール計画", OutputBehaviorType.STRATEGIC_PLAN),
    DAILY_PLANNING("하루 계획", "Daily Planning", "一日の計画", OutputBehaviorType.STRATEGIC_PLAN),
    WEEKLY_PLANNING("주간 계획", "Weekly Planning", "週間計画", OutputBehaviorType.STRATEGIC_PLAN),
    MONTHLY_PLANNING("월간 계획", "Monthly Planning", "月間計画", OutputBehaviorType.STRATEGIC_PLAN),
    TASK_AUTOMATION("작업 자동화", "Task Automation", "タスク自動化", OutputBehaviorType.STRATEGIC_PLAN),
    SHOPPING_LIST("쇼핑 리스트", "Shopping List", "買い物リスト", OutputBehaviorType.STRATEGIC_PLAN),
    MEAL_PLANNING("식사 계획", "Meal Planning", "食事計画", OutputBehaviorType.STRATEGIC_PLAN),
    BUDGET_PLANNING("예산 계획", "Budget Planning", "予算計画", OutputBehaviorType.STRATEGIC_PLAN),
    HOUSEHOLD_MANAGEMENT("가정 관리", "Household Management", "家事管理", OutputBehaviorType.STRATEGIC_PLAN),
    EFFICIENCY_ANALYSIS("효율성 분석", "Efficiency Analysis", "効率性分析", OutputBehaviorType.STRATEGIC_PLAN),
    PRODUCTIVITY_PLANNING("생산성 계획 수립", "Productivity Planning", "生産性計画策定", OutputBehaviorType.STRATEGIC_PLAN),
    PROCESS_IMPROVEMENT("프로세스 개선", "Process Improvement", "プロセス改善", OutputBehaviorType.STRATEGIC_PLAN),
    RESOURCE_OPTIMIZATION("자원 최적화", "Resource Optimization", "リソース最適化", OutputBehaviorType.STRATEGIC_PLAN);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.PRODUCTIVITY." + name();
    }

    @Override
    public Optional<TaskDomain> getTaskDomain() {
        return Optional.of(TaskDomain.PRACTICAL);
    }

    @Override
    public OutputBehaviorType getOutputBehavior() {
        return outputBehavior;
    }
}

