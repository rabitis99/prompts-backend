package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.literary.LiteraryCommand;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LiteraryCommandValidator implements ProductionValidator {

    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof LiteraryCommand literaryCommand)) {
            throw new ValidationException(
                    "Command is not instance of LiteraryCommand: " + command.getClass().getName());
        }
        if (literaryCommand.literaryType() == null) {
            throw new ValidationException("literaryType must not be null");
        }
        log.debug("LiteraryCommand validation passed - literaryType: {}", literaryCommand.literaryType());
    }

    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.LITERARY;
    }
}
