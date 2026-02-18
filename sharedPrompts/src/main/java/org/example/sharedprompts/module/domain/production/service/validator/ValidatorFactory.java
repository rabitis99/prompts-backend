package org.example.sharedprompts.module.domain.production.service.validator;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidatorFactory {

    private final BlogResponseValidator blogResponseValidator;
    private final EmailResponseValidator emailResponseValidator;
    private final DocumentResponseValidator documentResponseValidator;
    private final ContentResponseValidator contentResponseValidator;

    public ResponseValidator getValidator(ProductionCommandType commandType) {
        return switch (commandType) {
            case BLOG -> blogResponseValidator;
            case EMAIL -> emailResponseValidator;
            case DOCUMENT -> documentResponseValidator;
            default -> contentResponseValidator;
        };
    }
}

