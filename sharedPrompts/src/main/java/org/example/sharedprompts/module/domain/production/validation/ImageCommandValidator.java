package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.springframework.stereotype.Component;

/**
 * ImageCommand 검증기
 */
@Component
@Slf4j
public class ImageCommandValidator implements ProductionValidator {
    
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 4096;
    
    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof ImageCommand imageCommand)) {
            throw new ValidationException(
                    "Command is not instance of ImageCommand: " + command.getClass().getName());
        }

        if (imageCommand.prompt() == null || imageCommand.prompt().isBlank()) {
            throw new ValidationException("prompt must not be blank");
        }
        
        if (imageCommand.width() < MIN_SIZE || imageCommand.width() > MAX_SIZE) {
            throw new ValidationException(
                    String.format("width must be between %d and %d, but was %d", 
                            MIN_SIZE, MAX_SIZE, imageCommand.width()));
        }
        
        if (imageCommand.height() < MIN_SIZE || imageCommand.height() > MAX_SIZE) {
            throw new ValidationException(
                    String.format("height must be between %d and %d, but was %d", 
                            MIN_SIZE, MAX_SIZE, imageCommand.height()));
        }
        
        validateAspectRatio(imageCommand.width(), imageCommand.height());
        
        log.debug("ImageCommand validation passed - prompt: {}, size: {}x{}", 
                imageCommand.prompt(), imageCommand.width(), imageCommand.height());
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.IMAGE;
    }
    
    /**
     * 종횡비 검증
     */
    private void validateAspectRatio(int width, int height) {
        // 최소/최대 비율 제한 (예: 1:4 ~ 4:1)
        double ratio = (double) width / height;
        if (ratio < 0.25 || ratio > 4.0) {
            throw new ValidationException(
                    String.format("Aspect ratio must be between 1:4 and 4:1, but was %.2f:1", ratio));
        }
    }
}

