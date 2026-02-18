package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ValidatorRegistry {
    
    private final Map<ProductionCommandType, ProductionValidator> validatorMap = new ConcurrentHashMap<>();
    
    public ValidatorRegistry(List<ProductionValidator> validators) {
        for (ProductionValidator validator : validators) {
            ProductionCommandType supportedType = findSupportedType(validator);
            if (supportedType != null) {
                validatorMap.put(supportedType, validator);
                log.info("Registered Validator: {} for CommandType: {}", 
                        validator.getClass().getSimpleName(), supportedType);
            }
        }
    }
    
    private ProductionCommandType findSupportedType(ProductionValidator validator) {
        return Arrays.stream(ProductionCommandType.values())
                .filter(validator::supports)
                .findFirst()
                .orElse(null);
    }
    
    public void validate(ProductionCommand command) {
        ProductionCommandType commandType = command.getCommandType();
        ProductionValidator validator = validatorMap.get(commandType);
        
        if (validator == null) {
            throw new ValidationException(
                    "No validator found for CommandType: " + commandType);
        }
        
        validator.validate(command);
    }
}

