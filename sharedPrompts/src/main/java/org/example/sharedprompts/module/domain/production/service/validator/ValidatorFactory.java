package org.example.sharedprompts.module.domain.production.service.validator;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

/**
 * CommandType → ResponseValidator 매핑 제공
 */
@Component
@RequiredArgsConstructor
public class ValidatorFactory {

    private final BlogValidator blogValidator;
    private final EmailValidator emailValidator;
    private final DocumentValidator documentValidator;
    private final DefaultValidator defaultValidator;

    /**
     * CommandType에 해당하는 Validator 반환
     */
    public ResponseValidator getValidator(ProductionCommandType commandType) {
        return switch (commandType) {
            case BLOG -> blogValidator;
            case EMAIL -> emailValidator;
            case DOCUMENT -> documentValidator;
            default -> defaultValidator;
        };
    }
}

