package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;

/**
 * Port for the recommendation-only flow (POST /prompts/recommend).
 * Returns recommended semantic axes without generating a prompt.
 */
public interface RecommendPromptAxesUseCase {

    RecommendPromptResult recommend(RecommendPromptCommand command);
}
