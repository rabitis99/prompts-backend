package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TextCommandValidator implements ProductionValidator {
    
    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof TextCommand textCommand)) {
            throw new ValidationException(
                    "Command is not instance of TextCommand: " + command.getClass().getName());
        }

        if (textCommand.fileName() == null || textCommand.fileName().isBlank()) {
            throw new ValidationException("fileName must not be blank");
        }
        
        FormatValidator.validateFormatNotNullOrBlank(textCommand.format());
        FormatValidator.validateTextFormat(textCommand.format());
        
        log.debug("TextCommand validation passed - fileName: {}, format: {}", 
                textCommand.fileName(), textCommand.format());
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.TEXT;
    }
}

