package org.example.sharedprompts.domain.prompt.application.port.in.recommend;

import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;

/** 추천 전용 유즈케이스. 추천 축만 반환, 생성 없음 */
public interface RecommendPromptAxesUseCase {

    RecommendPromptResult recommend(RecommendPromptCommand command);
}
