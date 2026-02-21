package org.example.sharedprompts.module.domain.production.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FormatValidator 유틸리티 테스트")
class FormatValidatorTest {

    // ===== validateFormatNotNullOrBlank() =====

    @Test
    @DisplayName("유효한 포맷 문자열은 예외 없이 통과한다")
    void validateFormatNotNullOrBlank_valid_string_passes() {
        assertThatCode(() -> FormatValidator.validateFormatNotNullOrBlank("pdf"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 포맷은 ValidationException이 발생한다")
    void validateFormatNotNullOrBlank_null_throws_exception() {
        assertThatThrownBy(() -> FormatValidator.validateFormatNotNullOrBlank(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("format");
    }

    @Test
    @DisplayName("빈 문자열 포맷은 ValidationException이 발생한다")
    void validateFormatNotNullOrBlank_empty_throws_exception() {
        assertThatThrownBy(() -> FormatValidator.validateFormatNotNullOrBlank(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("format");
    }

    @Test
    @DisplayName("공백만 있는 포맷은 ValidationException이 발생한다")
    void validateFormatNotNullOrBlank_blank_throws_exception() {
        assertThatThrownBy(() -> FormatValidator.validateFormatNotNullOrBlank("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("format");
    }

    // ===== validateDocumentFormat() =====

    @ParameterizedTest
    @ValueSource(strings = {"md", "markdown", "pdf", "html", "json", "txt", "csv", "xml"})
    @DisplayName("지원되는 문서 포맷은 예외 없이 통과한다")
    void validateDocumentFormat_supported_formats_pass(String format) {
        assertThatCode(() -> FormatValidator.validateDocumentFormat(format))
                .as("Document format '%s' should pass", format)
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"docx", "ppt", "xlsx", "rtf", "odt", "zip"})
    @DisplayName("지원하지 않는 문서 포맷은 ValidationException이 발생한다")
    void validateDocumentFormat_unsupported_formats_throw_exception(String format) {
        assertThatThrownBy(() -> FormatValidator.validateDocumentFormat(format))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(format);
    }

    @Test
    @DisplayName("문서 포맷이 null이면 ValidationException이 발생한다")
    void validateDocumentFormat_null_throws_exception() {
        assertThatThrownBy(() -> FormatValidator.validateDocumentFormat(null))
                .isInstanceOf(ValidationException.class);
    }

    // ===== validateTextFormat() =====

    @ParameterizedTest
    @ValueSource(strings = {"txt", "md", "markdown", "html", "json", "csv", "pdf", "xml"})
    @DisplayName("지원되는 텍스트 포맷은 예외 없이 통과한다")
    void validateTextFormat_supported_formats_pass(String format) {
        assertThatCode(() -> FormatValidator.validateTextFormat(format))
                .as("Text format '%s' should pass", format)
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ppt", "docx", "exe", "png"})
    @DisplayName("지원하지 않는 텍스트 포맷은 ValidationException이 발생한다")
    void validateTextFormat_unsupported_formats_throw_exception(String format) {
        assertThatThrownBy(() -> FormatValidator.validateTextFormat(format))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(format);
    }

    @Test
    @DisplayName("텍스트 포맷이 null이면 ValidationException이 발생한다")
    void validateTextFormat_null_throws_exception() {
        assertThatThrownBy(() -> FormatValidator.validateTextFormat(null))
                .isInstanceOf(ValidationException.class);
    }
}
