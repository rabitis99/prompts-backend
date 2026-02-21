package org.example.sharedprompts.module.domain.production.validation;

import org.example.sharedprompts.module.domain.production.model.executor.blog.BlogCommand;
import org.example.sharedprompts.module.domain.production.model.executor.document.DocumentCommand;
import org.example.sharedprompts.module.domain.production.model.executor.email.EmailCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidatorRegistry 테스트")
class ValidatorRegistryTest {

    @Test
    @DisplayName("등록된 커맨드 타입은 정상 검증된다")
    void validate_registered_command_type_passes() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of(new BlogCommandValidator()));
        BlogCommand command = new BlogCommand("유효한 제목", List.of("tag"));

        assertThatCode(() -> registry.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("등록되지 않은 커맨드 타입으로 validate() 호출 시 ValidationException이 발생한다")
    void validate_unregistered_command_type_throws_exception() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of(new BlogCommandValidator()));
        EmailCommand emailCommand = new EmailCommand("제목", "user@example.com");

        assertThatThrownBy(() -> registry.validate(emailCommand))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No validator found");
    }

    @Test
    @DisplayName("여러 Validator 등록 시 각 타입에 맞는 Validator로 라우팅된다")
    void multiple_validators_route_to_correct_validator() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of(
                new BlogCommandValidator(),
                new EmailCommandValidator(),
                new DocumentCommandValidator()
        ));

        assertThatCode(() -> registry.validate(new BlogCommand("제목", List.of("tag"))))
                .doesNotThrowAnyException();
        assertThatCode(() -> registry.validate(new EmailCommand("제목", "user@example.com")))
                .doesNotThrowAnyException();
        assertThatCode(() -> registry.validate(new DocumentCommand("file", "pdf")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("등록된 Validator의 검증 실패 시 ValidationException이 전파된다")
    void validate_propagates_validation_failure() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of(new BlogCommandValidator()));
        BlogCommand invalidCommand = new BlogCommand(null, List.of("tag")); // title 없음

        assertThatThrownBy(() -> registry.validate(invalidCommand))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("중복 Validator 등록 시 첫 번째가 유지되어 정상 동작한다")
    void duplicate_validator_registration_retains_first_and_still_validates() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of(
                new BlogCommandValidator(),
                new BlogCommandValidator()  // 중복
        ));
        BlogCommand command = new BlogCommand("제목", List.of());

        assertThatCode(() -> registry.validate(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 Validator 목록으로 생성 후 validate() 호출 시 ValidationException이 발생한다")
    void empty_registry_throws_exception_on_validate() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of());
        BlogCommand command = new BlogCommand("제목", List.of());

        assertThatThrownBy(() -> registry.validate(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No validator found");
    }
}
