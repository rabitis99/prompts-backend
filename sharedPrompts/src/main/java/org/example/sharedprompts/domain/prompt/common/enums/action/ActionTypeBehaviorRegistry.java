package org.example.sharedprompts.domain.prompt.common.enums.action;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;

/**
 * 세분화된 ActionType enum들을 코어 {@link OutputBehaviorType}으로 매핑하는 레지스트리.
 *
 * <p>하위 enum의 내용을 크게 변경하지 않고도,
 * 엔진 레벨에서는 일관된 "출력 행동" 시그널을 사용할 수 있도록 한다.</p>
 */
public final class ActionTypeBehaviorRegistry {

    private ActionTypeBehaviorRegistry() {
    }

    public static OutputBehaviorType resolveBehavior(ActionTypeInterface actionType) {
        if (actionType == null) {
            return OutputBehaviorType.GENERAL_CONSULTATION;
        }

        // 생산성/실무 계획 계열
        if (actionType instanceof ProductivityActionType
                || actionType instanceof PersonalDevelopmentActionType
                || actionType instanceof BusinessActionType
                || actionType instanceof CareerActionType
                || actionType instanceof ShoppingActionType) {
            return OutputBehaviorType.STRATEGIC_PLAN;
        }

        // 개발/엔지니어링/인프라/보안/AI 계열 — 코드 및 기술 구현 중심
        if (actionType instanceof CodingActionType
                || actionType instanceof ProgrammingActionType
                || actionType instanceof DevelopmentActionType
                || actionType instanceof DevOpsActionType
                || actionType instanceof CloudServicesActionType
                || actionType instanceof CybersecurityActionType
                || actionType instanceof AiMlActionType) {
            return OutputBehaviorType.CODE_IMPLEMENTATION;
        }

        // 분석/연구 계열 — 리포트/데이터 분석 중심
        if (actionType instanceof AnalysisActionType
                || actionType instanceof ResearchActionType) {
            TaskDomain domain = actionType.getTaskDomain().orElse(TaskDomain.ANALYTICAL);
            return domain == TaskDomain.ANALYTICAL
                    ? OutputBehaviorType.ANALYTICAL_REPORT
                    : OutputBehaviorType.DATA_ANALYSIS;
        }

        // 소셜 — 도메인에 따라 STRATEGIC_PLAN vs LONG_FORM_WRITING
        if (actionType instanceof SocialActionType) {
            TaskDomain domain = actionType.getTaskDomain().orElse(TaskDomain.GENERAL);
            return domain == TaskDomain.PRACTICAL
                    ? OutputBehaviorType.STRATEGIC_PLAN
                    : OutputBehaviorType.LONG_FORM_WRITING;
        }

        // 콘텐츠/글쓰기 계열 — 장문/카피/스크립트 작성
        if (actionType instanceof WritingActionType
                || actionType instanceof ContentActionType
                || actionType instanceof EmailActionType
                || actionType instanceof RecommendationActionType) {
            // 콘텐츠 액션 내부에는 장문/단문이 혼재하지만,
            // 엔진 레벨에서는 일단 장문 작성 중심 시그널을 사용하고,
            // 상위 레이어에서 출처(ActionType 이름)를 추가 힌트로 사용한다.
            return OutputBehaviorType.LONG_FORM_WRITING;
        }

        // 디자인/크리에이티브 계열 — 창의적 산출물
        if (actionType instanceof CreativeActionType
                || actionType instanceof DesignActionType) {
            return OutputBehaviorType.LONG_FORM_WRITING;
        }

        // 학습/교육 계열 — 단계별 설명/튜토리얼
        if (actionType instanceof StudyActionType
                || actionType instanceof EducationActionType) {
            return OutputBehaviorType.EDUCATIONAL_EXPLANATION;
        }

        // 헬스/라이프스타일 — 실용 조언 + 계획
        if (actionType instanceof HealthFitnessActionType
                || actionType instanceof LifestyleActionType) {
            return OutputBehaviorType.STRATEGIC_PLAN;
        }

        // 고객 지원 계열 — FAQ/가이드/응대 템플릿
        if (actionType instanceof CustomerSupportActionType) {
            return OutputBehaviorType.QA_STYLE_RESPONSE;
        }

        // 기타/상담 계열 — 일반 상담/설명
        if (actionType instanceof EtcActionType) {
            return OutputBehaviorType.GENERAL_CONSULTATION;
        }

        // 매핑 누락 시 보수적 기본값
        return OutputBehaviorType.GENERAL_CONSULTATION;
    }
}

