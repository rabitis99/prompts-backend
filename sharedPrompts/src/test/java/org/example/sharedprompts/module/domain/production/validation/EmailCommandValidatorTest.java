package org.example.sharedprompts.module.domain.production.validation;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.email.EmailCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EmailCommandValidator 테스트")
class EmailCommandValidatorTest {

    private final EmailCommandValidator validator = new EmailCommandValidator();

    @Test
    @DisplayName("유효한 EmailCommand는 예외 없이 통과한다")
    void validate_valid_email_command_passes() {
        EmailCommand command = new EmailCommand("주문 확인 메일", "user@example.com");

        assertThatCode(() -> validator.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("subject가 null이면 ValidationException이 발생한다")
    void validate_null_subject_throws_exception() {
        EmailCommand command = new EmailCommand(null, "user@example.com");

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("subject");
    }

    @Test
    @DisplayName("subject가 공백이면 ValidationException이 발생한다")
    void validate_blank_subject_throws_exception() {
        EmailCommand command = new EmailCommand("  ", "user@example.com");

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("subject");
    }

    @Test
    @DisplayName("recipient가 null이면 ValidationException이 발생한다")
    void validate_null_recipient_throws_exception() {
        EmailCommand command = new EmailCommand("제목", null);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("recipient");
    }

    @Test
    @DisplayName("recipient가 공백이면 ValidationException이 발생한다")
    void validate_blank_recipient_throws_exception() {
        EmailCommand command = new EmailCommand("제목", "  ");

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("recipient");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-an-email",
            "userexample.com",
            "user@",
            "@example.com",
            "user@.com",
            "user@exam_ple.com",
            "user name@example.com"
    })
    @DisplayName("잘못된 이메일 형식이면 ValidationException이 발생한다")
    void validate_invalid_email_format_throws_exception(String invalidEmail) {
        EmailCommand command = new EmailCommand("제목", invalidEmail);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.co.kr",
            "user123@sub.domain.com"
    })
    @DisplayName("유효한 이메일 형식은 통과한다")
    void validate_valid_email_formats_pass(String validEmail) {
        EmailCommand command = new EmailCommand("제목", validEmail);

        assertThatCode(() -> validator.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("EmailCommand가 아닌 커맨드가 전달되면 ValidationException이 발생한다")
    void validate_wrong_command_type_throws_exception() {
        TextCommand textCommand = new TextCommand("file.txt", "txt");

        assertThatThrownBy(() -> validator.validate(textCommand))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("EmailCommand");
    }

    @Test
    @DisplayName("supports()는 EMAIL 타입에 대해 true를 반환한다")
    void supports_email_returns_true() {
        assertThat(validator.supports(ProductionCommandType.EMAIL)).isTrue();
    }

    @Test
    @DisplayName("supports()는 EMAIL 이외 타입에 대해 false를 반환한다")
    void supports_non_email_returns_false() {
        assertThat(validator.supports(ProductionCommandType.TEXT)).isFalse();
        assertThat(validator.supports(ProductionCommandType.BLOG)).isFalse();
    }
}
