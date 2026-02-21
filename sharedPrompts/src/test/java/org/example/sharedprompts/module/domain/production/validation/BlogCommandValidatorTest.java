package org.example.sharedprompts.module.domain.production.validation;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.blog.BlogCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BlogCommandValidator 테스트")
class BlogCommandValidatorTest {

    private final BlogCommandValidator validator = new BlogCommandValidator();

    @Test
    @DisplayName("유효한 BlogCommand는 예외 없이 통과한다")
    void validate_valid_blog_command_passes() {
        BlogCommand command = new BlogCommand("Spring Boot 활용법", List.of("java", "spring"));

        assertThatCode(() -> validator.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("title이 null이면 ValidationException이 발생한다")
    void validate_null_title_throws_exception() {
        BlogCommand command = new BlogCommand(null, List.of("java"));

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("title이 빈 문자열이면 ValidationException이 발생한다")
    void validate_empty_title_throws_exception() {
        BlogCommand command = new BlogCommand("", List.of("java"));

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("title이 공백만 있으면 ValidationException이 발생한다")
    void validate_blank_title_throws_exception() {
        BlogCommand command = new BlogCommand("   ", List.of("java"));

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("tags가 null이면 ValidationException이 발생한다")
    void validate_null_tags_throws_exception() {
        BlogCommand command = new BlogCommand("유효한 제목", null);

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("tags");
    }

    @Test
    @DisplayName("tags가 빈 리스트이면 통과한다")
    void validate_empty_tags_list_passes() {
        BlogCommand command = new BlogCommand("유효한 제목", List.of());

        assertThatCode(() -> validator.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BlogCommand가 아닌 커맨드가 전달되면 ValidationException이 발생한다")
    void validate_wrong_command_type_throws_exception() {
        TextCommand textCommand = new TextCommand("file.txt", "txt");

        assertThatThrownBy(() -> validator.validate(textCommand))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("BlogCommand");
    }

    @Test
    @DisplayName("supports()는 BLOG 타입에 대해 true를 반환한다")
    void supports_blog_returns_true() {
        assertThat(validator.supports(ProductionCommandType.BLOG)).isTrue();
    }

    @Test
    @DisplayName("supports()는 BLOG 이외 타입에 대해 false를 반환한다")
    void supports_non_blog_returns_false() {
        assertThat(validator.supports(ProductionCommandType.TEXT)).isFalse();
        assertThat(validator.supports(ProductionCommandType.EMAIL)).isFalse();
        assertThat(validator.supports(ProductionCommandType.DOCUMENT)).isFalse();
        assertThat(validator.supports(ProductionCommandType.IMAGE)).isFalse();
    }
}
