package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;

/**
 * Intent별 semantic resolution 기본값 (objective / output needs / response shape).
 * <p>
 * {@link org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent} enum에는 정책이 없고,
 * 실제 기본값은 {@link IntentDictionary}에서 제공한다.
 */
public record IntentResolutionDefaults(
        PromptObjective defaultObjective,
        OutputNeeds preferredOutputNeeds,
        ResponseShape defaultResponseShape
) {}

