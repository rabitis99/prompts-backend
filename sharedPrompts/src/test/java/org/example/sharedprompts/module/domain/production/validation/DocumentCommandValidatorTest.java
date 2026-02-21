package org.example.sharedprompts.module.domain.production.validation;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.document.DocumentCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DocumentCommandValidator 테스트")
class DocumentCommandValidatorTest {

    private final DocumentCommandValidator validator = new DocumentCommandValidator();

    @Test
    @DisplayName("유효한 DocumentCommand는 예외 없이 통과한다")
    void validate_valid_document_command_passes() {
        DocumentCommand command = new DocumentCommand("report", "pdf");

        assertThatCode(() -> validator.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("fileName이 null이면 ValidationException이 발생한다")
    void validate_null_fileName_throws_exception() {
        DocumentCommand command = new DocumentCommand(null, "pdf");

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("fileName");
    }

    @Test
    @DisplayName("fileName이 공백이면 ValidationException이 발생한다")
    void validate_blank_fileName_throws_exception() {
        DocumentCommand command = new DocumentCommand("  ", "pdf");

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("fileName");
    }

    @Test
    @DisplayName("format이 null이면 ValidationException이 발생한다")
    void validate_null_format_throws_exception() {
        DocumentCommand command = new DocumentCommand("report", null);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("format");
    }

    @Test
    @DisplayName("format이 공백이면 ValidationException이 발생한다")
    void validate_blank_format_throws_exception() {
        DocumentCommand command = new DocumentCommand("report", "  ");

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("format");
    }

    @ParameterizedTest
    @ValueSource(strings = {"docx", "ppt", "xlsx", "rtf", "odt"})
    @DisplayName("지원하지 않는 format이면 ValidationException이 발생한다")
    void validate_unsupported_format_throws_exception(String unsupportedFormat) {
        DocumentCommand command = new DocumentCommand("report", unsupportedFormat);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(unsupportedFormat);
    }

    @ParameterizedTest
    @ValueSource(strings = {"md", "markdown", "pdf", "html", "json", "txt", "csv", "xml"})
    @DisplayName("지원되는 모든 포맷은 예외 없이 통과한다")
    void validate_all_supported_formats_pass(String supportedFormat) {
        DocumentCommand command = new DocumentCommand("report", supportedFormat);

        assertThatCode(() -> validator.validate(command))
                .as("Format '%s' should pass validation", supportedFormat)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DocumentCommand가 아닌 커맨드가 전달되면 ValidationException이 발생한다")
    void validate_wrong_command_type_throws_exception() {
        TextCommand textCommand = new TextCommand("file.txt", "txt");

        assertThatThrownBy(() -> validator.validate(textCommand))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DocumentCommand");
    }

    @Test
    @DisplayName("supports()는 DOCUMENT 타입에 대해 true를 반환한다")
    void supports_document_returns_true() {
        assertThat(validator.supports(ProductionCommandType.DOCUMENT)).isTrue();
    }

    @Test
    @DisplayName("supports()는 DOCUMENT 이외 타입에 대해 false를 반환한다")
    void supports_non_document_returns_false() {
        assertThat(validator.supports(ProductionCommandType.TEXT)).isFalse();
        assertThat(validator.supports(ProductionCommandType.BLOG)).isFalse();
        assertThat(validator.supports(ProductionCommandType.EMAIL)).isFalse();
        assertThat(validator.supports(ProductionCommandType.IMAGE)).isFalse();
    }
}
