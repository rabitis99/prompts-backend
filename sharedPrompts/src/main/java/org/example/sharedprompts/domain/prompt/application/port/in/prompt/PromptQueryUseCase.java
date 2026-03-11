package org.example.sharedprompts.domain.prompt.application.port.in.prompt;

import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptDetailView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptPageResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.PromptSummaryView;
import org.example.sharedprompts.domain.prompt.application.port.in.query.SearchPromptsQuery;

/** 프롬프트 조회 유즈케이스. View/PageResult만 노출 */
public interface PromptQueryUseCase {

    PromptPageResult<PromptSummaryView> getPrompts(SearchPromptsQuery query);

    PromptDetailView getPromptDetail(Long promptId, Long viewerId);

    PromptPageResult<PromptSummaryView> getMyPrompts(SearchPromptsQuery query);

    PromptPageResult<PromptSummaryView> getUserPrompts(SearchPromptsQuery query);
}
