package org.example.sharedprompts.controller.prompt;

import org.example.sharedprompts.domain.prompt.domain.service.RecommendationRegistry;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromptMetadataController.class)
class PromptMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationRegistry recommendationRegistry;

    @Test
    @DisplayName("도메인 메타데이터 엔드포인트는 RecommendationRegistry를 통해 추천 톤/스타일을 반환한다")
    void domainMetadataUsesRecommendationRegistry() throws Exception {
        given(recommendationRegistry.getRecommendedStyles(TaskDomain.TECHNICAL))
                .willReturn(Set.of(StyleType.TECHNICAL));
        given(recommendationRegistry.getRecommendedTones(TaskDomain.TECHNICAL))
                .willReturn(Set.of(ToneType.PROFESSIONAL));

        mockMvc.perform(get("/prompts/metadata/domains/TECHNICAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domain").value("TECHNICAL"))
                .andExpect(jsonPath("$.data.display_name").isNotEmpty())
                .andExpect(jsonPath("$.data.recommended_styles[0]").value("TECHNICAL"))
                .andExpect(jsonPath("$.data.recommended_tones[0]").value("PROFESSIONAL"));

        then(recommendationRegistry).should().getRecommendedStyles(TaskDomain.TECHNICAL);
        then(recommendationRegistry).should().getRecommendedTones(TaskDomain.TECHNICAL);
    }

    @Test
    @DisplayName("톤 메타데이터 엔드포인트는 RecommendationRegistry를 통해 추천 도메인을 반환한다")
    void toneMetadataUsesRecommendationRegistry() throws Exception {
        given(recommendationRegistry.getRecommendedDomainsForTone(ToneType.FRIENDLY))
                .willReturn(Set.of(TaskDomain.GENERAL));

        mockMvc.perform(get("/prompts/metadata/tones/FRIENDLY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tone").value("FRIENDLY"))
                .andExpect(jsonPath("$.data.display_name").isNotEmpty())
                .andExpect(jsonPath("$.data.recommended_domains[0]").value("GENERAL"));

        then(recommendationRegistry).should().getRecommendedDomainsForTone(ToneType.FRIENDLY);
    }
}

