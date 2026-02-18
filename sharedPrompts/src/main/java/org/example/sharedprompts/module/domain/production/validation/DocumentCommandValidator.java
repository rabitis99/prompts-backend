package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.document.DocumentCommand;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocumentCommandValidator implements ProductionValidator {
    
    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof DocumentCommand documentCommand)) {
            throw new ValidationException(
                    "Command is not instance of DocumentCommand: " + command.getClass().getName());
        }

        if (documentCommand.fileName() == null || documentCommand.fileName().isBlank()) {
            throw new ValidationException("fileName must not be blank");
        }
        
        FormatValidator.validateFormatNotNullOrBlank(documentCommand.format());
        FormatValidator.validateDocumentFormat(documentCommand.format());
        
        log.debug("DocumentCommand validation passed - fileName: {}, format: {}", 
                documentCommand.fileName(), documentCommand.format());
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.DOCUMENT;
    }
}

