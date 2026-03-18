package org.example.sharedprompts.domain.prompt.infrastructure.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classpath JSON policy source loads stable key–based preferences correctly.
 */
@DisplayName("ClasspathJsonRecommendationPreferenceSource: stable key policy loading")
class ClasspathJsonRecommendationPreferenceSourceTest {

    @Test
    @DisplayName("Pre-loaded map returns preferred action keys by category+intent")
    void preloadedMapReturnsPreferredKeysByContext() {
        Map<String, List<String>> prefs = Map.of(
                "WRITING+GENERATE", List.of("ACTION.WRITING.ESSAY_WRITING", "ACTION.WRITING.ARTICLE_WRITING"),
                "DEVELOPMENT+GENERATE", List.of("ACTION.CODING.IMPLEMENTATION")
        );
        ClasspathJsonRecommendationPreferenceSource source =
                new ClasspathJsonRecommendationPreferenceSource(prefs, "test-json");

        List<String> writing = source.getPreferredActionKeys(PromptCategory.WRITING, ActionIntent.GENERATE);
        List<String> dev = source.getPreferredActionKeys(PromptCategory.DEVELOPMENT, ActionIntent.GENERATE);
        List<String> empty = source.getPreferredActionKeys(PromptCategory.WRITING, ActionIntent.REWRITE);

        assertThat(writing).containsExactly("ACTION.WRITING.ESSAY_WRITING", "ACTION.WRITING.ARTICLE_WRITING");
        assertThat(dev).containsExactly("ACTION.CODING.IMPLEMENTATION");
        assertThat(empty).isEmpty();
    }
}
