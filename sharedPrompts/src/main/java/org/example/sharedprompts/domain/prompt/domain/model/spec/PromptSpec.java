package org.example.sharedprompts.domain.prompt.domain.model.spec;

import lombok.Getter;
import org.example.sharedprompts.domain.prompt.application.port.out.persistence.SavePromptVersionPort;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.result.QualityRubric;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.strategy.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityPriority;
import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;

/**
 * 프롬프트 생성 명세 — 4단계 파이프라인(Clarify → Solve → Verify → Repair)의 입력이다.
 *
 * <p>이 클래스는 순수 도메인 모델이며 JPA 엔티티와 분리된다.
 * 생성 완료 후 결과를 {@code Prompt} JPA 엔티티로 저장하는 것은
 * {@link SavePromptVersionPort}가 담당한다.
 */
@Getter
public final class PromptSpec {

    // ─── 목적/품질 ───
    private final PromptObjective objective;
    private final QualityPriority priority;
    private final QualityRubric rubric;

    // ─── 도메인/난이도 ───
    private final TaskDomain taskDomain;
    private final ExperienceLevel experienceLevel;

    // ─── 섹션/구조 ───
    private final List<PromptSection> sections;
    private final Constraints constraints;
    private final OutputContract outputContract;
    private final ContentSandbox contentSandbox;

    // ─── 스타일 ───
    private final RoleTypeInterface role;
    private final ToneType tone;
    private final StyleType style;

    // ─── 전략 ───
    private final PromptStrategyBundle strategyBundle;

    // ─── 언어/입력 ───
    private final LanguageType locale;
    private final String rawInput;             // 사용자 원본 입력(Clarify 이전)
    private final String clarifiedInput;       // Clarify 단계 이후 정제된 입력
    private final ActionTypeInterface actionType;

    private PromptSpec(Builder builder) {
        this.objective = requireNonNull(builder.objective, "objective");
        this.priority = requireNonNull(builder.priority, "priority");
        this.rubric = requireNonNull(builder.rubric, "rubric");
        this.taskDomain = builder.taskDomain;
        this.experienceLevel = builder.experienceLevel != null ? builder.experienceLevel : ExperienceLevel.INTERMEDIATE;
        this.sections = builder.sections != null ? List.copyOf(builder.sections) : List.of();
        this.constraints = builder.constraints != null ? builder.constraints : Constraints.defaults();
        this.outputContract = builder.outputContract != null
                ? builder.outputContract
                : OutputContract.freeText(2000);
        this.contentSandbox = builder.contentSandbox != null
                ? builder.contentSandbox
                : ContentSandbox.defaults();
        this.role = builder.role;
        this.tone = builder.tone != null ? builder.tone : ToneType.NEUTRAL;
        this.style = builder.style != null ? builder.style : StyleType.NARRATIVE;
        this.strategyBundle = requireNonNull(builder.strategyBundle, "strategyBundle");
        this.locale = builder.locale != null ? builder.locale : LanguageType.KOREAN;
        this.rawInput = requireNonNull(builder.rawInput, "rawInput");
        this.clarifiedInput = builder.clarifiedInput != null ? builder.clarifiedInput : builder.rawInput;
        this.actionType = builder.actionType;
    }

    private static <T> T requireNonNull(T val, String name) {
        if (val == null) throw new IllegalArgumentException("PromptSpec의 " + name + "은 null일 수 없습니다.");
        return val;
    }

    /** Clarify 단계 완료 후 clarifiedInput이 반영된 새 PromptSpec 반환 */
    public PromptSpec withClarifiedInput(String clarifiedInput) {
        return toBuilder().clarifiedInput(clarifiedInput).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .objective(objective).priority(priority).rubric(rubric)
                .taskDomain(taskDomain).experienceLevel(experienceLevel)
                .sections(sections).constraints(constraints).outputContract(outputContract)
                .contentSandbox(contentSandbox).role(role).tone(tone).style(style)
                .strategyBundle(strategyBundle).locale(locale)
                .rawInput(rawInput).clarifiedInput(clarifiedInput).actionType(actionType);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PromptObjective objective;
        private QualityPriority priority;
        private QualityRubric rubric;
        private TaskDomain taskDomain;
        private ExperienceLevel experienceLevel;
        private List<PromptSection> sections;
        private Constraints constraints;
        private OutputContract outputContract;
        private ContentSandbox contentSandbox;
        private RoleTypeInterface role;
        private ToneType tone;
        private StyleType style;
        private PromptStrategyBundle strategyBundle;
        private LanguageType locale;
        private String rawInput;
        private String clarifiedInput;
        private ActionTypeInterface actionType;

        public Builder objective(PromptObjective v) { objective = v; return this; }
        public Builder priority(QualityPriority v) { priority = v; return this; }
        public Builder rubric(QualityRubric v) { rubric = v; return this; }
        public Builder taskDomain(TaskDomain v) { taskDomain = v; return this; }
        public Builder experienceLevel(ExperienceLevel v) { experienceLevel = v; return this; }
        public Builder sections(List<PromptSection> v) { sections = v; return this; }
        public Builder constraints(Constraints v) { constraints = v; return this; }
        public Builder outputContract(OutputContract v) { outputContract = v; return this; }
        public Builder contentSandbox(ContentSandbox v) { contentSandbox = v; return this; }
        public Builder role(RoleTypeInterface v) { role = v; return this; }
        public Builder tone(ToneType v) { tone = v; return this; }
        public Builder style(StyleType v) { style = v; return this; }
        public Builder strategyBundle(PromptStrategyBundle v) { strategyBundle = v; return this; }
        public Builder locale(LanguageType v) { locale = v; return this; }
        public Builder rawInput(String v) { rawInput = v; return this; }
        public Builder clarifiedInput(String v) { clarifiedInput = v; return this; }
        public Builder actionType(ActionTypeInterface v) { actionType = v; return this; }

        public PromptSpec build() { return new PromptSpec(this); }
    }
}
