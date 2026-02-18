package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.blog.BlogCommand;
import org.springframework.stereotype.Component;

/**
 * BlogCommand 검증기
 */
@Component
@Slf4j
public class BlogCommandValidator implements ProductionValidator {
    
    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof BlogCommand blogCommand)) {
            throw new ValidationException(
                    "Command is not instance of BlogCommand: " + command.getClass().getName());
        }

        if (blogCommand.title() == null || blogCommand.title().isBlank()) {
            throw new ValidationException("title must not be blank");
        }
        
        if (blogCommand.tags() == null) {
            throw new ValidationException("tags must not be null");
        }
        
        log.debug("BlogCommand validation passed - title: {}, tags: {}", 
                blogCommand.title(), blogCommand.tags());
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.BLOG;
    }
}

