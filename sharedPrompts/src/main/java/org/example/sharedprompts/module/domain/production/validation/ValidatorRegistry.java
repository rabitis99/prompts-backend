package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validator 레지스트리
 * Command 타입별로 적절한 Validator를 찾아 검증 수행
 */
@Component
@Slf4j
public class ValidatorRegistry {
    
    private final Map<ProductionCommandType, ProductionValidator> validatorMap = new ConcurrentHashMap<>();
    
    public ValidatorRegistry(List<ProductionValidator> validators) {
        // 모든 Validator 구현체를 등록
        for (ProductionValidator validator : validators) {
            for (ProductionCommandType commandType : ProductionCommandType.values()) {
                if (validator.supports(commandType)) {
                    validatorMap.put(commandType, validator);
                    log.info("Registered Validator: {} for CommandType: {}", 
                            validator.getClass().getSimpleName(), commandType);
                    break;
                }
            }
        }
    }
    
    /**
     * Command 검증 수행
     */
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

