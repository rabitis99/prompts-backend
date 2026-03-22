package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fully resolved semantic axes after profile lookup, recommendation, and validation.
 * This is the only input the renderer and spec builder receive for semantic axes—
 * no hidden inference from category alone.
 */
public record ConfirmedSemanticAxes(
        PromptCategory category,
        TaskDomain taskDomain,
        ActionIntent intent,
        PromptObjective objective,
        OutputNeeds outputNeeds,
        Optional<RoleTypeInterface> role,
        Optional<ActionTypeInterface> actionType,
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experienceLevel,
        List<String> appliedProfileIds,
        List<String> validationWarnings,
        List<String> recommendationHints
) {
    public ConfirmedSemanticAxes {
        category = Objects.requireNonNull(category, "category");
        taskDomain = Objects.requireNonNull(taskDomain, "taskDomain");
        intent = Objects.requireNonNull(intent, "intent");
        objective = Objects.requireNonNull(objective, "objective");
        outputNeeds = Objects.requireNonNull(outputNeeds, "outputNeeds");
        role = role != null ? role : Optional.empty();
        actionType = actionType != null ? actionType : Optional.empty();
        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experienceLevel = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
        appliedProfileIds = appliedProfileIds != null ? List.copyOf(appliedProfileIds) : List.of();
        validationWarnings = validationWarnings != null ? List.copyOf(validationWarnings) : List.of();
        recommendationHints = recommendationHints != null ? List.copyOf(recommendationHints) : List.of();
    }

    /**
     * Permissive lookup: optional axes may omit {@link #actionType}; then no capability group exists for rendering.
     * Uses {@link CanonicalActionRegistry#toCanonical(ActionTypeInterface)} — empty when type is absent or lookup fails.
     *
     * <p>{@link org.example.sharedprompts.domain.prompt.domain.service.spec.PromptSpecFactory} applies a stricter rule:
     * when {@link #actionType} is present, the registry must resolve a group (no direct enum read there).
     */
    public Optional<ActionGroup> findActionGroup(CanonicalActionRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return actionType.flatMap(registry::toCanonical);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PromptCategory category;
        private TaskDomain taskDomain;
        private ActionIntent intent;
        private PromptObjective objective;
        private OutputNeeds outputNeeds;
        private RoleTypeInterface role;
        private ActionTypeInterface actionType;
        private ToneType tone = ToneType.NEUTRAL;
        private StyleType style = StyleType.NARRATIVE;
        private LanguageType language = LanguageType.KOREAN;
        private ExperienceLevel experienceLevel = ExperienceLevel.INTERMEDIATE;
        private List<String> appliedProfileIds = List.of();
        private List<String> validationWarnings = List.of();
        private List<String> recommendationHints = List.of();

        public Builder category(PromptCategory v) { category = v; return this; }
        public Builder taskDomain(TaskDomain v) { taskDomain = v; return this; }
        public Builder intent(ActionIntent v) { intent = v; return this; }
        public Builder objective(PromptObjective v) { objective = v; return this; }
        public Builder outputNeeds(OutputNeeds v) { outputNeeds = v; return this; }
        public Builder role(RoleTypeInterface v) { role = v; return this; }
        public Builder actionType(ActionTypeInterface v) { actionType = v; return this; }
        public Builder tone(ToneType v) { tone = v != null ? v : ToneType.NEUTRAL; return this; }
        public Builder style(StyleType v) { style = v != null ? v : StyleType.NARRATIVE; return this; }
        public Builder language(LanguageType v) { language = v != null ? v : LanguageType.KOREAN; return this; }
        public Builder experienceLevel(ExperienceLevel v) { experienceLevel = v != null ? v : ExperienceLevel.INTERMEDIATE; return this; }
        public Builder appliedProfileIds(List<String> v) { appliedProfileIds = v != null ? List.copyOf(v) : List.of(); return this; }
        public Builder validationWarnings(List<String> v) { validationWarnings = v != null ? List.copyOf(v) : List.of(); return this; }
        public Builder recommendationHints(List<String> v) { recommendationHints = v != null ? List.copyOf(v) : List.of(); return this; }

        public ConfirmedSemanticAxes build() {
            return new ConfirmedSemanticAxes(
                    category,
                    taskDomain,
                    intent,
                    objective,
                    outputNeeds,
                    Optional.ofNullable(role),
                    Optional.ofNullable(actionType),
                    tone,
                    style,
                    language,
                    experienceLevel,
                    appliedProfileIds,
                    validationWarnings,
                    recommendationHints
            );
        }
    }
}
