package org.example.sharedprompts.domain.prompt.common.enums.action.category.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * 커리어/취업 관련 액션 타입 enum
 *
 * <p>생성자 파라미터 순서 (모든 파라미터는 String 타입):
 * <ol>
 *   <li>stableKey - 안정 키 (직렬화/호환성용)</li>
 *   <li>displayNameKo - 표시 이름 (한국어)</li>
 *   <li>displayNameEn - 표시 이름 (영어)</li>
 *   <li>displayNameJa - 표시 이름 (일본어)</li>
 * </ol>
 */
@Getter
@AllArgsConstructor
public enum CareerActionType implements ActionTypeInterface, StableKeyedEnum {
    RESUME_WRITING("이력서 작성", "Resume Writing", "履歴書作成", OutputBehaviorType.STRATEGIC_PLAN),
    COVER_LETTER("자기소개서", "Cover Letter", "カバーレター", OutputBehaviorType.STRATEGIC_PLAN),
    INTERVIEW_PREPARATION("면접 준비", "Interview Preparation", "面接準備", OutputBehaviorType.STRATEGIC_PLAN),
    NETWORKING_MESSAGE("네트워킹 메시지", "Networking Message", "ネットワーキングメッセージ", OutputBehaviorType.STRATEGIC_PLAN),
    APPLICATION_WRITING("지원서 작성", "Application Writing", "応募書類作成", OutputBehaviorType.STRATEGIC_PLAN);

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
    private final OutputBehaviorType outputBehavior;

    @Override
    public String key() {
        return "ACTION.CAREER." + name();
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

