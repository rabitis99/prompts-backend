package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation") // intentional: tests legacy enum values for backward compatibility
@DisplayName("PromptCategory taxonomy and legacy compatibility")
class PromptCategoryTaxonomyTest {

    @Nested
    @DisplayName("canonical()")
    class Canonical {
        @Test
        void legacySALES_mapsToMARKETING() {
            assertThat(PromptCategory.SALES.canonical()).isEqualTo(PromptCategory.MARKETING);
        }

        @Test
        void legacyOPERATIONS_mapsToBUSINESS() {
            assertThat(PromptCategory.OPERATIONS.canonical()).isEqualTo(PromptCategory.BUSINESS);
        }

        @Test
        void legacyCONTENT_CREATION_mapsToWRITING() {
            assertThat(PromptCategory.CONTENT_CREATION.canonical()).isEqualTo(PromptCategory.WRITING);
        }

        @Test
        void legacyANALYSIS_mapsToDATA_ANALYSIS() {
            assertThat(PromptCategory.ANALYSIS.canonical()).isEqualTo(PromptCategory.DATA_ANALYSIS);
        }

        @Test
        void canonicalCategories_returnSelf() {
            assertThat(PromptCategory.PRODUCTIVITY.canonical()).isEqualTo(PromptCategory.PRODUCTIVITY);
            assertThat(PromptCategory.MARKETING.canonical()).isEqualTo(PromptCategory.MARKETING);
            assertThat(PromptCategory.WRITING.canonical()).isEqualTo(PromptCategory.WRITING);
            assertThat(PromptCategory.DATA_ANALYSIS.canonical()).isEqualTo(PromptCategory.DATA_ANALYSIS);
            assertThat(PromptCategory.EXTRACTION.canonical()).isEqualTo(PromptCategory.EXTRACTION);
            assertThat(PromptCategory.ETC.canonical()).isEqualTo(PromptCategory.ETC);
        }

        @Test
        void CREATIVE_returnsSelf() {
            assertThat(PromptCategory.CREATIVE.canonical()).isEqualTo(PromptCategory.CREATIVE);
        }
    }

    @Nested
    @DisplayName("isLegacy()")
    class IsLegacy {
        @Test
        void legacyCategories_returnTrue() {
            assertThat(PromptCategory.SALES.isLegacy()).isTrue();
            assertThat(PromptCategory.OPERATIONS.isLegacy()).isTrue();
            assertThat(PromptCategory.CONTENT_CREATION.isLegacy()).isTrue();
            assertThat(PromptCategory.ANALYSIS.isLegacy()).isTrue();
        }

        @Test
        void nonLegacy_returnFalse() {
            assertThat(PromptCategory.PRODUCTIVITY.isLegacy()).isFalse();
            assertThat(PromptCategory.MARKETING.isLegacy()).isFalse();
            assertThat(PromptCategory.CREATIVE.isLegacy()).isFalse();
            assertThat(PromptCategory.EXTRACTION.isLegacy()).isFalse();
        }
    }

    @Nested
    @DisplayName("isUiSelectable()")
    class IsUiSelectable {
        @Test
        void canonicalUiCategories_returnTrue() {
            assertThat(PromptCategory.PRODUCTIVITY.isUiSelectable()).isTrue();
            assertThat(PromptCategory.BUSINESS.isUiSelectable()).isTrue();
            assertThat(PromptCategory.MARKETING.isUiSelectable()).isTrue();
            assertThat(PromptCategory.CUSTOMER_SUPPORT.isUiSelectable()).isTrue();
            assertThat(PromptCategory.DEVELOPMENT.isUiSelectable()).isTrue();
            assertThat(PromptCategory.DATA_ANALYSIS.isUiSelectable()).isTrue();
            assertThat(PromptCategory.RESEARCH.isUiSelectable()).isTrue();
            assertThat(PromptCategory.EDUCATION.isUiSelectable()).isTrue();
            assertThat(PromptCategory.WRITING.isUiSelectable()).isTrue();
            assertThat(PromptCategory.DESIGN.isUiSelectable()).isTrue();
            assertThat(PromptCategory.LEGAL.isUiSelectable()).isTrue();
        }

        @Test
        void EXTRACTION_and_ETC_notUiSelectable() {
            assertThat(PromptCategory.EXTRACTION.isUiSelectable()).isFalse();
            assertThat(PromptCategory.ETC.isUiSelectable()).isFalse();
        }

        @Test
        void legacyAndCREATIVE_notUiSelectable() {
            assertThat(PromptCategory.SALES.isUiSelectable()).isFalse();
            assertThat(PromptCategory.OPERATIONS.isUiSelectable()).isFalse();
            assertThat(PromptCategory.CONTENT_CREATION.isUiSelectable()).isFalse();
            assertThat(PromptCategory.ANALYSIS.isUiSelectable()).isFalse();
            assertThat(PromptCategory.CREATIVE.isUiSelectable()).isFalse();
        }
    }

    @Nested
    @DisplayName("serialization keys unchanged")
    class Keys {
        @Test
        void legacyKeys_unchanged() {
            assertThat(PromptCategory.SALES.key()).isEqualTo("PROMPT_CATEGORY.SALES");
            assertThat(PromptCategory.OPERATIONS.key()).isEqualTo("PROMPT_CATEGORY.OPERATIONS");
            assertThat(PromptCategory.CONTENT_CREATION.key()).isEqualTo("PROMPT_CATEGORY.CONTENT_CREATION");
            assertThat(PromptCategory.ANALYSIS.key()).isEqualTo("PROMPT_CATEGORY.ANALYSIS");
        }
    }
}
