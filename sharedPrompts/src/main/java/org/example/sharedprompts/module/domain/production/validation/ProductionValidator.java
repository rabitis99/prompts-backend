package org.example.sharedprompts.module.domain.production.validation;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

public interface ProductionValidator {
    void validate(ProductionCommand command) throws ValidationException;
    boolean supports(ProductionCommandType commandType);
}

